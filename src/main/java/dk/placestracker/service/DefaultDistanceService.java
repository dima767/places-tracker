package dk.placestracker.service;

import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.Settings;
import dk.placestracker.domain.repository.PlaceRepository;
import dk.placestracker.util.DistanceCalculator;
import dk.placestracker.web.dto.DistanceMatrixResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default implementation of DistanceService using Google Distance Matrix API.
 * Caches driving distances on Place entities and uses Haversine formula as fallback.
 *
 * @author Dmitriy Kopylenko
 */
@Service
public class DefaultDistanceService implements DistanceService {

    private static final Logger log = LoggerFactory.getLogger(DefaultDistanceService.class);
    private static final String DISTANCE_MATRIX_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    private final SettingsService settingsService;
    private final PlaceRepository placeRepository;
    private final RestClient restClient;
    private final String apiKey;
    private final boolean apiKeyConfigured;
    private final int batchSize;

    public DefaultDistanceService(
            SettingsService settingsService,
            PlaceRepository placeRepository,
            RestClient.Builder restClientBuilder,
            @Value("${google.maps.api-key}") String apiKey,
            @Value("${google.maps.distance-matrix.batch-size:25}") int batchSize) {

        this.settingsService = settingsService;
        this.placeRepository = placeRepository;
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();
        this.batchSize = batchSize;

        if (!apiKeyConfigured) {
            log.warn("Google Maps API key not configured. Driving distances will use Haversine fallback.");
        }
    }

    @Override
    public DistanceResult getDistance(Place place) {
        Settings settings = settingsService.getSettings();

        // Check if home location is configured
        if (!settings.hasHomeLocation()) {
            return DistanceResult.unavailable();
        }

        // Check if place has coordinates
        if (place.latitude() == null || place.longitude() == null) {
            return DistanceResult.unavailable();
        }

        String homeLatLng = formatLatLng(settings.homeLatitude(), settings.homeLongitude());

        // Check cache validity
        if (place.hasValidCachedDistance(homeLatLng)) {
            log.debug("Using cached distance for place: {}", place.name());
            return DistanceResult.cached(place.drivingDistanceMiles(), place.drivingDurationMinutes());
        }

        // API key not configured - use fallback
        if (!apiKeyConfigured) {
            return calculateFallbackDistance(place, settings);
        }

        // Call API for single place
        try {
            DistanceMatrixResponse response = callDistanceMatrixApi(
                    homeLatLng,
                    formatLatLng(place.latitude(), place.longitude())
            );

            if (response != null && response.isOk()) {
                DistanceMatrixResponse.Element element = response.getElement(0);
                if (element != null && element.isOk()) {
                    double miles = element.distance().toMiles();
                    double minutes = element.duration().toMinutes();

                    // Update cache on place
                    Place updatedPlace = place.withDrivingDistance(miles, minutes, homeLatLng);
                    placeRepository.save(updatedPlace);

                    log.debug("API distance for {}: {} mi, {} min", place.name(), miles, minutes);
                    return DistanceResult.fromApi(miles, minutes);
                } else {
                    log.warn("Distance Matrix API element status: {} for {}",
                            element != null ? element.status() : "null element", place.name());
                }
            } else {
                log.warn("Distance Matrix API response status: {} for {}",
                        response != null ? response.status() : "null response", place.name());
            }

            // API call succeeded but no valid result - use fallback
            log.warn("Distance Matrix API returned no valid result for {}, using fallback", place.name());
            return calculateFallbackDistance(place, settings);

        } catch (Exception e) {
            log.error("Error calling Distance Matrix API for {}: {}", place.name(), e.getMessage());
            return calculateFallbackDistance(place, settings);
        }
    }

    @Override
    public Map<String, DistanceResult> getDistances(List<Place> places) {
        Map<String, DistanceResult> results = new HashMap<>();

        if (places == null || places.isEmpty()) {
            return results;
        }

        Settings settings = settingsService.getSettings();

        // Check if home location is configured
        if (!settings.hasHomeLocation()) {
            for (Place place : places) {
                results.put(place.id(), DistanceResult.unavailable());
            }
            return results;
        }

        String homeLatLng = formatLatLng(settings.homeLatitude(), settings.homeLongitude());

        // Separate places into cached, no-coords, and need-api
        List<Place> needApiCall = new ArrayList<>();

        for (Place place : places) {
            if (place.latitude() == null || place.longitude() == null) {
                results.put(place.id(), DistanceResult.unavailable());
            } else if (place.hasValidCachedDistance(homeLatLng)) {
                results.put(place.id(), DistanceResult.cached(
                        place.drivingDistanceMiles(),
                        place.drivingDurationMinutes()
                ));
            } else {
                needApiCall.add(place);
            }
        }

        // If API key not configured, use fallback for all remaining
        if (!apiKeyConfigured) {
            for (Place place : needApiCall) {
                results.put(place.id(), calculateFallbackDistance(place, settings));
            }
            return results;
        }

        // Batch API calls
        List<List<Place>> batches = partition(needApiCall, batchSize);
        for (List<Place> batch : batches) {
            processBatch(batch, homeLatLng, settings, results);
        }

        return results;
    }

    @Override
    public void invalidateAllDistances() {
        log.info("Invalidating all cached driving distances");

        List<Place> allPlaces = placeRepository.findAll();
        int cleared = 0;

        for (Place place : allPlaces) {
            if (place.drivingDistanceMiles() != null) {
                Place clearedPlace = place.withClearedDrivingDistance();
                placeRepository.save(clearedPlace);
                cleared++;
            }
        }

        log.info("Cleared driving distance cache for {} places", cleared);
    }

    private void processBatch(List<Place> batch, String homeLatLng, Settings settings,
                              Map<String, DistanceResult> results) {
        if (batch.isEmpty()) {
            return;
        }

        // Build destinations string (pipe-separated lat,lng pairs)
        String destinations = batch.stream()
                .map(p -> formatLatLng(p.latitude(), p.longitude()))
                .collect(Collectors.joining("|"));

        try {
            DistanceMatrixResponse response = callDistanceMatrixApi(homeLatLng, destinations);

            if (response != null && response.isOk()) {
                for (int i = 0; i < batch.size(); i++) {
                    Place place = batch.get(i);
                    DistanceMatrixResponse.Element element = response.getElement(i);

                    if (element != null && element.isOk()) {
                        double miles = element.distance().toMiles();
                        double minutes = element.duration().toMinutes();

                        // Update cache on place
                        Place updatedPlace = place.withDrivingDistance(miles, minutes, homeLatLng);
                        placeRepository.save(updatedPlace);

                        results.put(place.id(), DistanceResult.fromApi(miles, minutes));
                    } else {
                        // Individual destination failed - use fallback
                        results.put(place.id(), calculateFallbackDistance(place, settings));
                    }
                }
            } else {
                // Batch failed - use fallback for all
                log.warn("Distance Matrix API batch failed (status: {}), using fallback for {} places",
                        response != null ? response.status() : "null response", batch.size());
                for (Place place : batch) {
                    results.put(place.id(), calculateFallbackDistance(place, settings));
                }
            }
        } catch (Exception e) {
            log.error("Error calling Distance Matrix API for batch: {}", e.getMessage());
            for (Place place : batch) {
                results.put(place.id(), calculateFallbackDistance(place, settings));
            }
        }
    }

    private DistanceMatrixResponse callDistanceMatrixApi(String origin, String destinations) {
        return restClient.get()
                .uri(DISTANCE_MATRIX_API_URL + "?origins={origins}&destinations={destinations}&units=imperial&key={key}",
                        origin, destinations, apiKey)
                .retrieve()
                .body(DistanceMatrixResponse.class);
    }

    private DistanceResult calculateFallbackDistance(Place place, Settings settings) {
        Double miles = DistanceCalculator.calculateDistance(
                settings.homeLatitude(),
                settings.homeLongitude(),
                place.latitude(),
                place.longitude()
        );

        if (miles != null) {
            log.debug("Fallback distance for {}: {} mi", place.name(), miles);
            return DistanceResult.fallback(miles);
        }
        return DistanceResult.unavailable();
    }

    private String formatLatLng(Double lat, Double lng) {
        return lat + "," + lng;
    }

    /**
     * Partition a list into batches of specified size.
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
