package dk.placestracker.service;

import dk.placestracker.domain.model.Review;
import dk.placestracker.web.dto.PlaceExtractResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of GoogleMapsService using Google Places API (New).
 * Uses Spring Boot's RestClient for clean, modern HTTP calls.
 *
 * @author Dmitriy Kopylenko
 */
@Service
public class DefaultGoogleMapsService implements GoogleMapsService {

    private static final Logger log = LoggerFactory.getLogger(DefaultGoogleMapsService.class);

    // Google Places API (New) endpoints
    private static final String PLACES_API_BASE_URL = "https://places.googleapis.com/v1";

    // Patterns for extracting data from URLs
    private static final Pattern PLACE_ID_PATTERN = Pattern.compile("place_id=([^&]+)");
    private static final Pattern PLACE_NAME_PATTERN = Pattern.compile("/place/([^/@]+)");
    private static final Pattern COORDINATES_PATTERN = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");

    private final String apiKey;
    private final boolean apiKeyConfigured;
    private final RestClient restClient;

    public DefaultGoogleMapsService(
            @Value("${google.maps.api-key}") String apiKey,
            RestClient.Builder restClientBuilder) {

        this.apiKey = apiKey;
        this.apiKeyConfigured = apiKey != null && !apiKey.isBlank();

        // Build RestClient with base URL and default headers
        this.restClient = restClientBuilder
                .baseUrl(PLACES_API_BASE_URL)
                .defaultHeader("Content-Type", "application/json")
                .build();

        if (apiKeyConfigured) {
            log.info("Google Maps API (New) initialized successfully");
        } else {
            log.warn("Google Maps API key not configured. Auto-fill feature will be disabled.");
        }
    }

    @Override
    public PlaceExtractResponse extractPlaceFromUrl(String mapsUrl) {
        // 1. Validate input
        if (mapsUrl == null || mapsUrl.isBlank()) {
            return PlaceExtractResponse.error("Please enter a Google Maps URL");
        }

        if (!apiKeyConfigured) {
            return PlaceExtractResponse.error("Google Maps integration is not configured. Please contact the administrator.");
        }

        try {
            // 2. Resolve shortened URLs
            String resolvedUrl = resolveShortUrl(mapsUrl);
            log.debug("Resolved URL: {}", resolvedUrl);

            // 3. Try to extract place_id first (if present)
            String placeId = extractPlaceId(resolvedUrl);

            Map<String, Object> placeDetails;

            if (placeId != null) {
                log.debug("Extracted place ID: {}", placeId);
                // 4a. Fetch place details using place_id
                placeDetails = fetchPlaceDetails(placeId);
            } else {
                // 4b. Extract place name and use Text Search
                String placeName = extractPlaceName(resolvedUrl);
                if (placeName == null) {
                    return PlaceExtractResponse.error("Could not extract place information from URL. Please ensure it's a valid Google Maps place link.");
                }

                log.debug("Extracted place name: {}", placeName);
                placeDetails = searchPlaceByText(placeName, resolvedUrl);
            }

            // 5. Map to our response format
            return mapToPlaceExtractResponse(placeDetails);

        } catch (Exception e) {
            log.error("Error extracting place from URL: {}", mapsUrl, e);
            return handleException(e);
        }
    }

    private String resolveShortUrl(String url) throws Exception {
        // Check if it's a shortened URL
        if (url.contains("goo.gl") || url.contains("maps.app.goo.gl")) {
            log.debug("Resolving shortened URL: {}", url);

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("GET");

            String location = connection.getHeaderField("Location");
            connection.disconnect();

            if (location != null && !location.isBlank()) {
                log.debug("Shortened URL resolved to: {}", location);
                return location;
            }
        }
        return url;
    }

    private String extractPlaceId(String url) {
        // Try to extract place_id parameter (only present in some URL formats)
        Matcher placeMatcher = PLACE_ID_PATTERN.matcher(url);
        if (placeMatcher.find()) {
            return placeMatcher.group(1);
        }
        return null;
    }

    private String extractPlaceName(String url) {
        // Extract place name from /place/Name+With+Spaces/ format
        Matcher matcher = PLACE_NAME_PATTERN.matcher(url);
        if (matcher.find()) {
            String name = matcher.group(1);
            // URL decode and replace + with spaces
            return name.replace("+", " ");
        }
        return null;
    }

    /**
     * Search for a place using Text Search API.
     * Documentation: https://developers.google.com/maps/documentation/places/web-service/text-search
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> searchPlaceByText(String placeName, String url) {
        log.debug("Searching for place by text: {}", placeName);

        // Try to extract coordinates from URL for location bias
        Map<String, Object> requestBody = Map.of("textQuery", placeName);

        Matcher coordsMatcher = COORDINATES_PATTERN.matcher(url);
        if (coordsMatcher.find()) {
            double lat = Double.parseDouble(coordsMatcher.group(1));
            double lng = Double.parseDouble(coordsMatcher.group(2));
            log.debug("Using location bias: {}, {}", lat, lng);

            requestBody = Map.of(
                "textQuery", placeName,
                "locationBias", Map.of(
                    "circle", Map.of(
                        "center", Map.of(
                            "latitude", lat,
                            "longitude", lng
                        ),
                        "radius", 5000.0  // 5km radius
                    )
                )
            );
        }

        Map<String, Object> response = restClient.post()
                .uri("/places:searchText")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.addressComponents,places.location,places.websiteUri,places.internationalPhoneNumber,places.rating,places.userRatingCount,places.reviews")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        // Extract first place from results
        if (response != null && response.containsKey("places")) {
            var places = (java.util.List<Map<String, Object>>) response.get("places");
            if (!places.isEmpty()) {
                return places.get(0);
            }
        }

        throw new RuntimeException("No places found for: " + placeName);
    }

    /**
     * Fetch place details using the new Places API with RestClient.
     * Documentation: https://developers.google.com/maps/documentation/places/web-service/place-details
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPlaceDetails(String placeId) {
        log.debug("Fetching place details for: {}", placeId);

        return restClient.get()
                .uri("/places/{placeId}", placeId)
                .header("X-Goog-Api-Key", apiKey)
                // Request specific fields to minimize quota usage
                .header("X-Goog-FieldMask", "id,displayName,formattedAddress,addressComponents,location,websiteUri,internationalPhoneNumber,rating,userRatingCount,reviews")
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private PlaceExtractResponse mapToPlaceExtractResponse(Map<String, Object> placeDetails) {
        try {
            // Extract display name
            String name = null;
            if (placeDetails.containsKey("displayName")) {
                Map<String, Object> displayName = (Map<String, Object>) placeDetails.get("displayName");
                if (displayName.containsKey("text")) {
                    name = (String) displayName.get("text");
                }
            }

            String city = null;
            String state = null;
            String country = null;
            Double lat = null;
            Double lng = null;
            String formattedAddress = null;
            String googlePlaceId = null;
            String website = null;
            String phoneNumber = null;
            Double googleRating = null;
            Integer googleReviewCount = null;

            // Extract Google Place ID
            if (placeDetails.containsKey("id")) {
                googlePlaceId = (String) placeDetails.get("id");
            }

            // Extract formatted address
            if (placeDetails.containsKey("formattedAddress")) {
                formattedAddress = (String) placeDetails.get("formattedAddress");
            }

            // Extract website
            if (placeDetails.containsKey("websiteUri")) {
                website = (String) placeDetails.get("websiteUri");
            }

            // Extract phone number
            if (placeDetails.containsKey("internationalPhoneNumber")) {
                phoneNumber = (String) placeDetails.get("internationalPhoneNumber");
            }

            // Extract rating
            if (placeDetails.containsKey("rating")) {
                googleRating = ((Number) placeDetails.get("rating")).doubleValue();
            }

            // Extract review count
            if (placeDetails.containsKey("userRatingCount")) {
                googleReviewCount = ((Number) placeDetails.get("userRatingCount")).intValue();
            }

            // Extract reviews
            List<Review> googleReviews = new ArrayList<>();
            if (placeDetails.containsKey("reviews")) {
                List<Map<String, Object>> reviewsList = (List<Map<String, Object>>) placeDetails.get("reviews");
                if (reviewsList != null) {
                    // Google returns up to 5 most relevant reviews by default
                    for (Map<String, Object> reviewData : reviewsList) {
                        try {
                            String authorName = null;
                            if (reviewData.containsKey("authorAttribution") && reviewData.get("authorAttribution") instanceof Map) {
                                Map<String, Object> author = (Map<String, Object>) reviewData.get("authorAttribution");
                                authorName = (String) author.get("displayName");
                            }

                            Integer rating = null;
                            if (reviewData.containsKey("rating")) {
                                rating = ((Number) reviewData.get("rating")).intValue();
                            }

                            String text = null;
                            if (reviewData.containsKey("text") && reviewData.get("text") instanceof Map) {
                                Map<String, Object> textObj = (Map<String, Object>) reviewData.get("text");
                                text = (String) textObj.get("text");
                            }

                            String relativeTime = (String) reviewData.get("relativePublishTimeDescription");

                            String profilePhotoUrl = null;
                            if (reviewData.containsKey("authorAttribution") && reviewData.get("authorAttribution") instanceof Map) {
                                Map<String, Object> author = (Map<String, Object>) reviewData.get("authorAttribution");
                                profilePhotoUrl = (String) author.get("photoUri");
                            }

                            LocalDateTime publishedAt = null;
                            if (reviewData.containsKey("publishTime")) {
                                String publishTime = (String) reviewData.get("publishTime");
                                publishedAt = LocalDateTime.ofInstant(
                                    Instant.parse(publishTime),
                                    ZoneId.systemDefault()
                                );
                            }

                            Review review = Review.create(authorName, rating, text, relativeTime, profilePhotoUrl, publishedAt);
                            googleReviews.add(review);
                        } catch (Exception e) {
                            log.warn("Error parsing review: {}", e.getMessage());
                            // Skip this review and continue with others
                        }
                    }
                }
            }

            // Extract coordinates
            if (placeDetails.containsKey("location")) {
                Map<String, Object> location = (Map<String, Object>) placeDetails.get("location");
                if (location.containsKey("latitude")) {
                    lat = ((Number) location.get("latitude")).doubleValue();
                }
                if (location.containsKey("longitude")) {
                    lng = ((Number) location.get("longitude")).doubleValue();
                }
            }

            // Parse address components
            if (placeDetails.containsKey("addressComponents")) {
                for (Object obj : (Iterable<?>) placeDetails.get("addressComponents")) {
                    Map<String, Object> component = (Map<String, Object>) obj;
                    Iterable<?> types = (Iterable<?>) component.get("types");

                    // Extract city/locality
                    if (hasType(types, "locality")) {
                        city = (String) component.get("longText");
                    }
                    // Extract state/province
                    if (hasType(types, "administrative_area_level_1")) {
                        state = (String) component.get("shortText");
                    }
                    // Extract country
                    if (hasType(types, "country")) {
                        country = (String) component.get("shortText");
                    }
                }
            }

            // Validate North American countries
            if (country != null && !isNorthAmerican(country)) {
                return PlaceExtractResponse.error(
                    "This location is not in North America. This tracker supports US, Canada, and Mexico only."
                );
            }

            // Create response
            PlaceExtractResponse.PlaceData data = new PlaceExtractResponse.PlaceData(
                name,
                city,
                state,
                country,
                lat,
                lng,
                formattedAddress,
                googlePlaceId,
                website,
                phoneNumber,
                googleRating,
                googleReviewCount,
                googleReviews
            );

            log.info("Successfully extracted place: {} ({}, {}, {}) - Rating: {} ({} reviews, {} fetched)",
                     name, city, state, country, googleRating, googleReviewCount, googleReviews.size());
            return PlaceExtractResponse.success(data);

        } catch (Exception e) {
            log.error("Error mapping place details to response", e);
            return PlaceExtractResponse.error("Error processing place information: " + e.getMessage());
        }
    }

    private boolean hasType(Iterable<?> types, String targetType) {
        if (types != null) {
            for (Object type : types) {
                if (type.toString().equals(targetType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNorthAmerican(String countryCode) {
        return "US".equals(countryCode) || "CA".equals(countryCode) || "MX".equals(countryCode);
    }

    private PlaceExtractResponse handleException(Exception e) {
        String message = e.getMessage();

        if (message != null) {
            if (message.contains("429") || message.contains("OVER_QUERY_LIMIT") || message.contains("OVER_DAILY_LIMIT")) {
                return PlaceExtractResponse.error("API quota exceeded. Please try again later.");
            } else if (message.contains("403") || message.contains("REQUEST_DENIED")) {
                return PlaceExtractResponse.error("API request denied. Please check API key configuration and ensure Places API (New) is enabled.");
            } else if (message.contains("400") || message.contains("INVALID_REQUEST")) {
                return PlaceExtractResponse.error("Invalid place ID. Please check the Google Maps URL.");
            } else if (message.contains("404") || message.contains("NOT_FOUND")) {
                return PlaceExtractResponse.error("Place not found. Please verify the Google Maps URL.");
            }
        }

        if (e instanceof java.net.SocketTimeoutException) {
            return PlaceExtractResponse.error("Request timeout. Please check your internet connection and try again.");
        }

        return PlaceExtractResponse.error("Failed to extract place information. Please try a different URL or try again later.");
    }
}
