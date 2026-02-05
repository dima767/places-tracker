package dk.placestracker.domain.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a visit to a place.
 * Each place can have multiple visits recorded over time.
 * Each visit can have multiple photos associated with it.
 *
 * @author Dmitriy Kopylenko
 */
public record Visit(
    @NotNull(message = "Visit ID is required")
    String id,

    @NotNull(message = "Visit date is required")
    LocalDate date,

    @DecimalMin(value = "-50.0", message = "Temperature must be at least -50°F")
    @DecimalMax(value = "150.0", message = "Temperature must not exceed 150°F")
    Double temperatureF,

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    String notes,

    // Duration of the visit (optional, e.g., "1h 25min")
    Duration duration,

    // Photo storage (GridFS file IDs)
    List<String> photoIds
) {
    /**
     * Factory method for creating new visits with auto-generated ID.
     *
     * @param date visit date
     * @param temperatureF temperature in Fahrenheit (optional)
     * @param notes visit notes (optional)
     * @param duration visit duration (optional)
     * @return new Visit instance with generated ID and empty photo list
     */
    public static Visit create(LocalDate date, Double temperatureF, String notes, Duration duration) {
        return new Visit(UUID.randomUUID().toString(), date, temperatureF, notes, duration, new ArrayList<>());
    }

    /**
     * Wither method for updating visit data while preserving ID and photos.
     *
     * @param date new visit date
     * @param temperatureF new temperature in Fahrenheit (optional)
     * @param notes new notes (optional)
     * @param duration visit duration (optional)
     * @return new Visit instance with updated data but same ID and photos
     */
    public Visit withUpdate(LocalDate date, Double temperatureF, String notes, Duration duration) {
        return new Visit(this.id, date, temperatureF, notes, duration, this.photoIds != null ? this.photoIds : new ArrayList<>());
    }

    /**
     * Wither method for adding a photo ID to this visit.
     *
     * @param photoId the GridFS file ID to add
     * @return new Visit instance with added photo
     */
    public Visit withAddedPhoto(String photoId) {
        List<String> newPhotoIds = new ArrayList<>(photoIds != null ? photoIds : new ArrayList<>());
        newPhotoIds.add(photoId);
        return new Visit(id, date, temperatureF, notes, duration, newPhotoIds);
    }

    /**
     * Wither method for removing a photo ID from this visit.
     *
     * @param photoId the GridFS file ID to remove
     * @return new Visit instance with photo removed
     */
    public Visit withRemovedPhoto(String photoId) {
        List<String> newPhotoIds = new ArrayList<>(photoIds != null ? photoIds : new ArrayList<>());
        newPhotoIds.remove(photoId);
        return new Visit(id, date, temperatureF, notes, duration, newPhotoIds);
    }

    /**
     * Wither method for replacing all photos.
     *
     * @param newPhotoIds the new list of photo IDs
     * @return new Visit instance with updated photos
     */
    public Visit withPhotos(List<String> newPhotoIds) {
        return new Visit(id, date, temperatureF, notes, duration, new ArrayList<>(newPhotoIds != null ? newPhotoIds : new ArrayList<>()));
    }

    /**
     * Helper method to get temperature in Celsius.
     * Formula: (F - 32) * 5/9
     *
     * @return temperature in Celsius, or null if temperatureF is null
     */
    public Double getTemperatureC() {
        if (temperatureF == null) return null;
        return (temperatureF - 32) * 5.0 / 9.0;
    }
}
