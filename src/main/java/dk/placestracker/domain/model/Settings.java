package dk.placestracker.domain.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Application settings stored as a singleton document in MongoDB.
 * Currently stores home location for distance calculations.
 *
 * @author Dmitriy Kopylenko
 */
@Document(collection = "settings")
public record Settings(
    @Id String id,
    @DecimalMin("-90.0") @DecimalMax("90.0") Double homeLatitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") Double homeLongitude,
    LocalDateTime updatedAt
) {
    private static final String DEFAULT_ID = "default";

    /**
     * Creates default settings with no home location configured.
     */
    public static Settings createDefault() {
        return new Settings(DEFAULT_ID, null, null, LocalDateTime.now());
    }

    /**
     * Checks if home location is configured.
     *
     * @return true if both latitude and longitude are set
     */
    public boolean hasHomeLocation() {
        return homeLatitude != null && homeLongitude != null;
    }

    /**
     * Creates a new Settings with updated home location.
     *
     * @param latitude  New home latitude
     * @param longitude New home longitude
     * @return New Settings instance with updated coordinates
     */
    public Settings withHomeLocation(Double latitude, Double longitude) {
        return new Settings(this.id, latitude, longitude, LocalDateTime.now());
    }

    /**
     * Creates a new Settings with cleared home location.
     *
     * @return New Settings instance with null coordinates
     */
    public Settings withClearedHomeLocation() {
        return new Settings(this.id, null, null, LocalDateTime.now());
    }
}
