package dk.placestracker.domain.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Main domain entity representing a place that can be visited.
 *
 * @author Dmitriy Kopylenko
 */
@Document(collection = "places")
public record Place(
        @Id
        String id,

        @NotBlank(message = "Place name is required")
        @Size(min = 2, max = 200, message = "Place name must be between 2 and 200 characters")
        @Indexed
        String name,

        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must not exceed 100 characters")
        @Indexed
        String state,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must not exceed 100 characters")
        String country,

        // Visit history (multiple visits per place)
        List<Visit> visits,

        // Facilities
        boolean hasToilet,

        Double latitude,

        Double longitude,

        // Google Maps extracted data
        String formattedAddress,

        String googlePlaceId,

        String website,

        String phoneNumber,

        Double googleRating,

        Integer googleReviewCount,

        // Google reviews (up to 5 most recent/helpful)
        List<Review> googleReviews,

        LocalDateTime createdAt,

        LocalDateTime updatedAt,

        // Driving distance cache fields
        Double drivingDistanceMiles,

        Double drivingDurationMinutes,

        LocalDateTime distanceCalculatedAt,

        /** Home location snapshot when distance was calculated (format: "lat,lng") */
        String distanceFromHomeLatLng
) {
    // Compact constructor for validation (optional)
    public Place {
        // Any custom validation logic can go here
    }

    // Helper factory method for creating new places (without id and timestamps)
    public static Place create(String name, String location, String state, String country,
                              List<Visit> visits, boolean hasToilet, Double latitude, Double longitude,
                              String formattedAddress, String googlePlaceId, String website, String phoneNumber,
                              Double googleRating, Integer googleReviewCount, List<Review> googleReviews) {
        return new Place(null, name, location, state, country,
                visits != null ? visits : new ArrayList<>(),
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                null, null,
                null, null, null, null);
    }

    // Wither method for setting timestamps (used by service layer)
    public Place withTimestamps(LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Place(id, name, location, state, country,
                visits != null ? visits : new ArrayList<>(),
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                drivingDistanceMiles, drivingDurationMinutes, distanceCalculatedAt, distanceFromHomeLatLng);
    }

    // Wither method for updates (preserves createdAt, updates updatedAt)
    public Place withUpdate(String name, String location, String state, String country,
                          List<Visit> visits, boolean hasToilet, Double latitude, Double longitude,
                          String formattedAddress, String googlePlaceId, String website, String phoneNumber,
                          Double googleRating, Integer googleReviewCount, LocalDateTime updatedAt) {
        return new Place(id, name, location, state, country,
                visits != null ? visits : new ArrayList<>(),
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                drivingDistanceMiles, drivingDurationMinutes, distanceCalculatedAt, distanceFromHomeLatLng);
    }

    // Visit management helper methods

    /**
     * Add a visit to this place. Visits are automatically sorted by date (most recent first).
     *
     * @param visit the visit to add
     * @return new Place instance with added visit
     */
    public Place withAddedVisit(Visit visit) {
        List<Visit> updatedVisits = new ArrayList<>(visits != null ? visits : new ArrayList<>());
        updatedVisits.add(visit);
        // Sort by date descending (most recent first)
        updatedVisits.sort(Comparator.comparing(Visit::date).reversed());
        return new Place(id, name, location, state, country, updatedVisits,
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                drivingDistanceMiles, drivingDurationMinutes, distanceCalculatedAt, distanceFromHomeLatLng);
    }

    /**
     * Remove a visit from this place by visit ID.
     *
     * @param visitId the ID of the visit to remove
     * @return new Place instance with visit removed
     */
    public Place withRemovedVisit(String visitId) {
        List<Visit> updatedVisits = new ArrayList<>(visits != null ? visits : new ArrayList<>());
        updatedVisits.removeIf(v -> v.id().equals(visitId));
        return new Place(id, name, location, state, country, updatedVisits,
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                drivingDistanceMiles, drivingDurationMinutes, distanceCalculatedAt, distanceFromHomeLatLng);
    }

    /**
     * Update a visit in this place. Visits are automatically re-sorted after update.
     *
     * @param visitId the ID of the visit to update
     * @param updatedVisit the updated visit data
     * @return new Place instance with updated visit
     */
    public Place withUpdatedVisit(String visitId, Visit updatedVisit) {
        List<Visit> updatedVisits = new ArrayList<>(visits != null ? visits : new ArrayList<>());
        for (int i = 0; i < updatedVisits.size(); i++) {
            if (updatedVisits.get(i).id().equals(visitId)) {
                updatedVisits.set(i, updatedVisit);
                break;
            }
        }
        // Re-sort after update
        updatedVisits.sort(Comparator.comparing(Visit::date).reversed());
        return new Place(id, name, location, state, country, updatedVisits,
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                drivingDistanceMiles, drivingDurationMinutes, distanceCalculatedAt, distanceFromHomeLatLng);
    }

    /**
     * Get the most recent visit to this place.
     *
     * @return Optional containing the most recent visit, or empty if no visits
     */
    public Optional<Visit> getMostRecentVisit() {
        if (visits == null || visits.isEmpty()) return Optional.empty();
        return visits.stream()
                .max(Comparator.comparing(Visit::date));
    }

    /**
     * Get the total number of visits to this place.
     *
     * @return visit count
     */
    public int getVisitCount() {
        return visits != null ? visits.size() : 0;
    }

    /**
     * Update this place with cached driving distance data.
     *
     * @param miles driving distance in miles
     * @param durationMinutes driving duration in minutes
     * @param homeLatLng snapshot of home location ("lat,lng" format)
     * @return new Place instance with updated driving distance cache
     */
    public Place withDrivingDistance(Double miles, Double durationMinutes, String homeLatLng) {
        return new Place(id, name, location, state, country,
                visits != null ? visits : new ArrayList<>(),
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                miles, durationMinutes, LocalDateTime.now(), homeLatLng);
    }

    /**
     * Clear cached driving distance data.
     *
     * @return new Place instance with cleared driving distance cache
     */
    public Place withClearedDrivingDistance() {
        return new Place(id, name, location, state, country,
                visits != null ? visits : new ArrayList<>(),
                hasToilet,
                latitude, longitude, formattedAddress, googlePlaceId, website, phoneNumber,
                googleRating, googleReviewCount, googleReviews != null ? googleReviews : new ArrayList<>(),
                createdAt, updatedAt,
                null, null, null, null);
    }

    /**
     * Check if this place has a valid cached driving distance for the current home location.
     *
     * @param currentHomeLatLng current home location in "lat,lng" format
     * @return true if cached distance is valid (home location matches)
     */
    public boolean hasValidCachedDistance(String currentHomeLatLng) {
        if (drivingDistanceMiles == null || distanceFromHomeLatLng == null) {
            return false;
        }
        return distanceFromHomeLatLng.equals(currentHomeLatLng);
    }
}
