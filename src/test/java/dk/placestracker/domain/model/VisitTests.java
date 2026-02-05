package dk.placestracker.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Visit Record Tests")
class VisitTests {

    @Test
    @DisplayName("create() should generate a new Visit with UUID")
    void testCreateGeneratesUuidId() {
        // Given
        LocalDate date = LocalDate.of(2024, 6, 15);
        Double temperatureF = 72.5;
        String notes = "Beautiful day at the park";

        // When
        Visit visit = Visit.create(date, temperatureF, notes, null);

        // Then
        assertThat(visit).isNotNull();
        assertThat(visit.id()).isNotNull();
        assertThat(visit.id()).isNotEmpty();
        assertThat(visit.date()).isEqualTo(date);
        assertThat(visit.temperatureF()).isEqualTo(temperatureF);
        assertThat(visit.notes()).isEqualTo(notes);
    }

    @Test
    @DisplayName("create() should allow null temperature")
    void testCreateWithNullTemperature() {
        // Given
        LocalDate date = LocalDate.of(2024, 6, 15);
        String notes = "Forgot to record temperature";

        // When
        Visit visit = Visit.create(date, null, notes, null);

        // Then
        assertThat(visit).isNotNull();
        assertThat(visit.temperatureF()).isNull();
        assertThat(visit.getTemperatureC()).isNull();
    }

    @Test
    @DisplayName("create() should allow null notes")
    void testCreateWithNullNotes() {
        // Given
        LocalDate date = LocalDate.of(2024, 6, 15);
        Double temperatureF = 85.0;

        // When
        Visit visit = Visit.create(date, temperatureF, null, null);

        // Then
        assertThat(visit).isNotNull();
        assertThat(visit.notes()).isNull();
    }

    @Test
    @DisplayName("withUpdate() should preserve ID and update other fields")
    void testWithUpdatePreservesId() {
        // Given
        Visit original = Visit.create(LocalDate.of(2024, 6, 15), 72.0, "Original notes", null);
        String originalId = original.id();

        // When
        Visit updated = original.withUpdate(
                LocalDate.of(2024, 6, 20),
                80.0,
                "Updated notes"
        , null);

        // Then
        assertThat(updated.id()).isEqualTo(originalId);
        assertThat(updated.date()).isEqualTo(LocalDate.of(2024, 6, 20));
        assertThat(updated.temperatureF()).isEqualTo(80.0);
        assertThat(updated.notes()).isEqualTo("Updated notes");
    }

    @Test
    @DisplayName("getTemperatureC() should correctly convert Fahrenheit to Celsius")
    void testTemperatureConversion() {
        // Given - Testing key conversion points
        Visit freezing = Visit.create(LocalDate.now(), 32.0, "Freezing point", null);
        Visit boiling = Visit.create(LocalDate.now(), 212.0, "Boiling point", null);
        Visit roomTemp = Visit.create(LocalDate.now(), 68.0, "Room temperature", null);
        Visit negativeF = Visit.create(LocalDate.now(), -40.0, "Same in both scales", null);

        // When & Then
        assertThat(freezing.getTemperatureC()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.1));
        assertThat(boiling.getTemperatureC()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
        assertThat(roomTemp.getTemperatureC()).isCloseTo(20.0, org.assertj.core.data.Offset.offset(0.1));
        assertThat(negativeF.getTemperatureC()).isCloseTo(-40.0, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    @DisplayName("getTemperatureC() should return null when temperatureF is null")
    void testTemperatureConversionWithNullInput() {
        // Given
        Visit visit = Visit.create(LocalDate.now(), null, "No temperature", null);

        // When
        Double celsius = visit.getTemperatureC();

        // Then
        assertThat(celsius).isNull();
    }

    @Test
    @DisplayName("getTemperatureC() should handle decimal temperatures accurately")
    void testDecimalTemperatureConversion() {
        // Given
        Visit visit = Visit.create(LocalDate.now(), 72.5, "Precise temperature", null);

        // When
        Double celsius = visit.getTemperatureC();

        // Then - (72.5 - 32) * 5/9 = 22.5°C
        assertThat(celsius).isCloseTo(22.5, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    @DisplayName("create() generates unique IDs for different visits")
    void testUniqueIdGeneration() {
        // Given & When
        Visit visit1 = Visit.create(LocalDate.now(), 70.0, "Visit 1", null);
        Visit visit2 = Visit.create(LocalDate.now(), 75.0, "Visit 2", null);

        // Then
        assertThat(visit1.id()).isNotEqualTo(visit2.id());
    }

    @Test
    @DisplayName("Visit records with same data (except ID) are not equal")
    void testRecordEquality() {
        // Given
        LocalDate date = LocalDate.of(2024, 6, 15);
        Visit visit1 = Visit.create(date, 72.0, "Same data", null);
        Visit visit2 = Visit.create(date, 72.0, "Same data", null);

        // Then - Different IDs mean different visits
        assertThat(visit1).isNotEqualTo(visit2);
    }

    @Test
    @DisplayName("Visit record with same ID and data should be equal")
    void testRecordEqualityWithSameId() {
        // Given
        Visit original = Visit.create(LocalDate.of(2024, 6, 15), 72.0, "Test", null);
        Visit copy = new Visit(original.id(), original.date(), original.temperatureF(), original.notes(), null, original.photoIds());

        // Then
        assertThat(original).isEqualTo(copy);
    }

    @Test
    @DisplayName("withUpdate() creates a new instance (immutability)")
    void testImmutability() {
        // Given
        Visit original = Visit.create(LocalDate.of(2024, 6, 15), 72.0, "Original", null);

        // When
        Visit updated = original.withUpdate(LocalDate.of(2024, 6, 20), 80.0, "Updated", null);

        // Then - Original unchanged
        assertThat(original.date()).isEqualTo(LocalDate.of(2024, 6, 15));
        assertThat(original.temperatureF()).isEqualTo(72.0);
        assertThat(original.notes()).isEqualTo("Original");

        // And updated is different instance
        assertThat(updated).isNotSameAs(original);
    }

    @Test
    @DisplayName("create() should initialize empty photoIds list")
    void testCreateInitializesEmptyPhotoIds() {
        // Given & When
        Visit visit = Visit.create(LocalDate.now(), 72.0, "Test visit", null);

        // Then
        assertThat(visit.photoIds()).isNotNull();
        assertThat(visit.photoIds()).isEmpty();
    }

    @Test
    @DisplayName("withAddedPhoto() should add photo ID to list")
    void testWithAddedPhoto() {
        // Given
        Visit visit = Visit.create(LocalDate.now(), 72.0, "Test visit", null);
        String photoId1 = "photo-id-123";
        String photoId2 = "photo-id-456";

        // When
        Visit withOnePhoto = visit.withAddedPhoto(photoId1);
        Visit withTwoPhotos = withOnePhoto.withAddedPhoto(photoId2);

        // Then
        assertThat(visit.photoIds()).isEmpty(); // Original unchanged
        assertThat(withOnePhoto.photoIds()).containsExactly(photoId1);
        assertThat(withTwoPhotos.photoIds()).containsExactly(photoId1, photoId2);
    }

    @Test
    @DisplayName("withRemovedPhoto() should remove photo ID from list")
    void testWithRemovedPhoto() {
        // Given
        Visit visit = Visit.create(LocalDate.now(), 72.0, "Test visit", null);
        String photoId1 = "photo-id-123";
        String photoId2 = "photo-id-456";
        Visit withPhotos = visit.withAddedPhoto(photoId1).withAddedPhoto(photoId2);

        // When
        Visit withOneRemoved = withPhotos.withRemovedPhoto(photoId1);

        // Then
        assertThat(withPhotos.photoIds()).containsExactly(photoId1, photoId2); // Original unchanged
        assertThat(withOneRemoved.photoIds()).containsExactly(photoId2);
    }

    @Test
    @DisplayName("withPhotos() should replace all photos")
    void testWithPhotos() {
        // Given
        Visit visit = Visit.create(LocalDate.now(), 72.0, "Test visit", null);
        Visit withOnePhoto = visit.withAddedPhoto("old-photo");
        List<String> newPhotos = List.of("photo-1", "photo-2", "photo-3");

        // When
        Visit replaced = withOnePhoto.withPhotos(newPhotos);

        // Then
        assertThat(withOnePhoto.photoIds()).containsExactly("old-photo"); // Original unchanged
        assertThat(replaced.photoIds()).containsExactly("photo-1", "photo-2", "photo-3");
    }

    @Test
    @DisplayName("withUpdate() should preserve photoIds")
    void testWithUpdatePreservesPhotoIds() {
        // Given
        Visit visit = Visit.create(LocalDate.of(2024, 6, 15), 72.0, "Original", null);
        Visit withPhotos = visit.withAddedPhoto("photo-1").withAddedPhoto("photo-2");

        // When
        Visit updated = withPhotos.withUpdate(LocalDate.of(2024, 6, 20), 80.0, "Updated", null);

        // Then
        assertThat(updated.photoIds()).containsExactly("photo-1", "photo-2");
        assertThat(updated.date()).isEqualTo(LocalDate.of(2024, 6, 20));
        assertThat(updated.temperatureF()).isEqualTo(80.0);
        assertThat(updated.notes()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("Photo methods should handle null photoIds gracefully")
    void testPhotoMethodsWithNullPhotoIds() {
        // Given - Create a visit with null photoIds (shouldn't happen, but test defensively)
        Visit visit = new Visit("test-id", LocalDate.now(), 72.0, "Test", null, null);

        // When & Then - Should not throw exceptions
        Visit withPhoto = visit.withAddedPhoto("photo-1");
        assertThat(withPhoto.photoIds()).containsExactly("photo-1");

        Visit removed = visit.withRemovedPhoto("non-existent");
        assertThat(removed.photoIds()).isEmpty();

        Visit replaced = visit.withPhotos(List.of("photo-2"));
        assertThat(replaced.photoIds()).containsExactly("photo-2");
    }
}
