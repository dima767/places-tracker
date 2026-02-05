package dk.placestracker.web.dto;

import dk.placestracker.domain.model.Review;
import java.util.List;

/**
 * Response DTO for Google Maps URL extraction.
 * Contains extracted place information or error details.
 *
 * @author Dmitriy Kopylenko
 */
public record PlaceExtractResponse(
    boolean success,
    String message,
    PlaceData data
) {
    public static PlaceExtractResponse success(PlaceData data) {
        return new PlaceExtractResponse(true, "Successfully extracted place information", data);
    }

    public static PlaceExtractResponse error(String message) {
        return new PlaceExtractResponse(false, message, null);
    }

    /**
     * Extracted place data.
     */
    public record PlaceData(
        String name,
        String location,           // city/locality
        String state,              // state code (e.g., "CA", "WY")
        String country,            // country code (e.g., "US", "CA", "MX")
        Double latitude,
        Double longitude,
        String formattedAddress,   // full address from Google
        String googlePlaceId,      // Google's unique place ID
        String website,            // official website URL
        String phoneNumber,        // contact phone number
        Double googleRating,       // rating 1-5 (null if no rating)
        Integer googleReviewCount, // number of Google reviews
        List<Review> googleReviews // up to 5 most recent/helpful reviews
    ) {}
}
