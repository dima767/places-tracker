package dk.placestracker.domain.model;

import java.time.LocalDateTime;

/**
 * Represents a Google review for a place.
 * This is embedded within the Place document in MongoDB.
 *
 * @author Dmitriy Kopylenko
 */
public record Review(
        String authorName,
        Integer rating,           // 1-5 star rating
        String text,              // Review text
        String relativeTime,      // e.g., "2 months ago"
        String profilePhotoUrl,   // Author's profile photo URL
        LocalDateTime publishedAt // When the review was published
) {
    /**
     * Factory method for creating a review
     */
    public static Review create(String authorName, Integer rating, String text,
                                String relativeTime, String profilePhotoUrl, LocalDateTime publishedAt) {
        return new Review(authorName, rating, text, relativeTime, profilePhotoUrl, publishedAt);
    }
}
