package dk.placestracker.service;

import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.Settings;
import dk.placestracker.domain.repository.PlaceRepository;
import dk.placestracker.web.dto.DistanceMatrixResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultDistanceService Tests")
class DefaultDistanceServiceTests {

    @Mock
    private SettingsService settingsService;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private DefaultDistanceService distanceService;

    private static final String API_KEY = "test-api-key";
    private static final double HOME_LAT = 40.7128;
    private static final double HOME_LNG = -74.0060;
    private static final String HOME_LAT_LNG = "40.7128,-74.006";

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        distanceService = new DefaultDistanceService(
                settingsService, placeRepository, restClientBuilder, API_KEY, 25
        );
    }

    private Place createPlace(String id, String name, double lat, double lng) {
        return new Place(
                id, name, "Location", "NY", "US",
                new ArrayList<>(), false, lat, lng,
                null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null
        );
    }

    private Place createPlaceWithCache(String id, String name, double lat, double lng,
                                        Double cachedMiles, Double cachedMinutes, String cachedHomeLatLng) {
        return new Place(
                id, name, "Location", "NY", "US",
                new ArrayList<>(), false, lat, lng,
                null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(),
                cachedMiles, cachedMinutes, LocalDateTime.now(), cachedHomeLatLng
        );
    }

    private Settings createSettingsWithHome() {
        return Settings.createDefault().withHomeLocation(HOME_LAT, HOME_LNG);
    }

    @Nested
    @DisplayName("getDistance() tests")
    class GetDistanceTests {

        @Test
        @DisplayName("Should return unavailable when no home location")
        void shouldReturnUnavailableWhenNoHomeLocation() {
            // Given
            when(settingsService.getSettings()).thenReturn(Settings.createDefault());
            Place place = createPlace("1", "Test Place", 42.3601, -71.0589);

            // When
            DistanceResult result = distanceService.getDistance(place);

            // Then
            assertThat(result.source()).isEqualTo(DistanceResult.Source.UNAVAILABLE);
            assertThat(result.hasDistance()).isFalse();
        }

        @Test
        @DisplayName("Should return unavailable when place has no coordinates")
        void shouldReturnUnavailableWhenNoCoordinates() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place place = new Place(
                    "1", "No Coords", "Location", "NY", "US",
                    new ArrayList<>(), false, null, null,
                    null, null, null, null, null, null, new ArrayList<>(),
                    LocalDateTime.now(), LocalDateTime.now(),
                    null, null, null, null
            );

            // When
            DistanceResult result = distanceService.getDistance(place);

            // Then
            assertThat(result.source()).isEqualTo(DistanceResult.Source.UNAVAILABLE);
        }

        @Test
        @DisplayName("Should return cached distance when home unchanged")
        void shouldReturnCachedDistanceWhenHomeUnchanged() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place place = createPlaceWithCache("1", "Cached Place", 42.3601, -71.0589,
                    200.5, 180.0, HOME_LAT_LNG);

            // When
            DistanceResult result = distanceService.getDistance(place);

            // Then
            assertThat(result.source()).isEqualTo(DistanceResult.Source.CACHED);
            assertThat(result.miles()).isEqualTo(200.5);
            assertThat(result.durationMinutes()).isEqualTo(180.0);
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("Should call API when cache is stale (home changed)")
        void shouldCallApiWhenCacheStale() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place place = createPlaceWithCache("1", "Stale Cache", 42.3601, -71.0589,
                    200.5, 180.0, "39.0,-75.0"); // Different home location

            // Setup API mock
            setupApiMock(createSuccessResponse(150.0, 120.0));
            when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

            // When
            DistanceResult result = distanceService.getDistance(place);

            // Then
            assertThat(result.source()).isEqualTo(DistanceResult.Source.API);
            assertThat(result.miles()).isCloseTo(150.0, org.assertj.core.api.Assertions.within(1.0));
            verify(placeRepository).save(any(Place.class));
        }

        @Test
        @DisplayName("Should fall back to Haversine when API fails")
        void shouldFallbackWhenApiFails() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place place = createPlace("1", "Test Place", 42.3601, -71.0589);

            // Setup API to throw exception
            when(restClient.get()).thenThrow(new RuntimeException("API Error"));

            // When
            DistanceResult result = distanceService.getDistance(place);

            // Then
            assertThat(result.source()).isEqualTo(DistanceResult.Source.FALLBACK);
            assertThat(result.hasDistance()).isTrue();
            assertThat(result.hasDuration()).isFalse(); // Haversine doesn't provide duration
        }
    }

    @Nested
    @DisplayName("getDistances() batch tests")
    class GetDistancesBatchTests {

        @Test
        @DisplayName("Should return empty map for empty list")
        void shouldReturnEmptyMapForEmptyList() {
            // When
            Map<String, DistanceResult> results = distanceService.getDistances(List.of());

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Should use cached values when available")
        void shouldUseCachedValuesWhenAvailable() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place cached1 = createPlaceWithCache("1", "Cached 1", 42.0, -71.0, 100.0, 90.0, HOME_LAT_LNG);
            Place cached2 = createPlaceWithCache("2", "Cached 2", 43.0, -72.0, 200.0, 180.0, HOME_LAT_LNG);

            // When
            Map<String, DistanceResult> results = distanceService.getDistances(List.of(cached1, cached2));

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get("1").source()).isEqualTo(DistanceResult.Source.CACHED);
            assertThat(results.get("2").source()).isEqualTo(DistanceResult.Source.CACHED);
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("Should mark places without coordinates as unavailable")
        void shouldMarkPlacesWithoutCoordsAsUnavailable() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place noCoords = new Place(
                    "1", "No Coords", "Location", "NY", "US",
                    new ArrayList<>(), false, null, null,
                    null, null, null, null, null, null, new ArrayList<>(),
                    LocalDateTime.now(), LocalDateTime.now(),
                    null, null, null, null
            );

            // When
            Map<String, DistanceResult> results = distanceService.getDistances(List.of(noCoords));

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get("1").source()).isEqualTo(DistanceResult.Source.UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("invalidateAllDistances() tests")
    class InvalidateAllDistancesTests {

        @Test
        @DisplayName("Should clear all cached distances")
        void shouldClearAllCachedDistances() {
            // Given
            Place cached1 = createPlaceWithCache("1", "Cached 1", 42.0, -71.0, 100.0, 90.0, HOME_LAT_LNG);
            Place cached2 = createPlaceWithCache("2", "Cached 2", 43.0, -72.0, 200.0, 180.0, HOME_LAT_LNG);
            Place noCache = createPlace("3", "No Cache", 44.0, -73.0);

            when(placeRepository.findAll()).thenReturn(List.of(cached1, cached2, noCache));
            when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

            // When
            distanceService.invalidateAllDistances();

            // Then
            verify(placeRepository, times(2)).save(any(Place.class)); // Only 2 had cached data
        }
    }

    @Nested
    @DisplayName("API key not configured tests")
    class NoApiKeyTests {

        @BeforeEach
        void setUp() {
            // Create service without API key
            distanceService = new DefaultDistanceService(
                    settingsService, placeRepository, restClientBuilder, "", 25
            );
        }

        @Test
        @DisplayName("Should use Haversine fallback when API key not configured")
        void shouldUseFallbackWhenNoApiKey() {
            // Given
            when(settingsService.getSettings()).thenReturn(createSettingsWithHome());
            Place place = createPlace("1", "Test Place", 42.3601, -71.0589);

            // When
            DistanceResult result = distanceService.getDistance(place);

            // Then
            assertThat(result.source()).isEqualTo(DistanceResult.Source.FALLBACK);
            assertThat(result.hasDistance()).isTrue();
            verifyNoInteractions(restClient);
        }
    }

    // Helper methods for API mocking

    private void setupApiMock(DistanceMatrixResponse response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(), any(), any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(DistanceMatrixResponse.class)).thenReturn(response);
    }

    private DistanceMatrixResponse createSuccessResponse(double miles, double minutes) {
        // Convert miles to meters and minutes to seconds for API response
        int meters = (int) (miles * 1609.344);
        int seconds = (int) (minutes * 60);

        return new DistanceMatrixResponse(
                "OK",
                List.of("Test Destination"),
                List.of("Test Origin"),
                List.of(new DistanceMatrixResponse.Row(
                        List.of(new DistanceMatrixResponse.Element(
                                "OK",
                                new DistanceMatrixResponse.Distance(meters, miles + " mi"),
                                new DistanceMatrixResponse.Duration(seconds, minutes + " mins")
                        ))
                ))
        );
    }
}
