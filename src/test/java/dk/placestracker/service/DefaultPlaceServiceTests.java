package dk.placestracker.service;

import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.Visit;
import dk.placestracker.domain.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultPlaceService Tests")
class DefaultPlaceServiceTests {

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private PhotoService photoService;

    @InjectMocks
    private DefaultPlaceService placeService;

    private Place testPlace;
    private Place savedPlace;

    @BeforeEach
    void setUp() {
        testPlace = Place.create(
                "Yosemite National Park",
                "Yosemite Valley",
                "California",
                "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 15), 75.0, "Beautiful waterfalls and granite cliffs", null)),
                false,
                37.8651,
                -119.5383,
                null, null, null, null, null, null, new ArrayList<>()
        );

        savedPlace = testPlace.withTimestamps(
                LocalDateTime.of(2024, 6, 15, 10, 0),
                LocalDateTime.of(2024, 6, 15, 10, 0)
        );
    }

    @Test
    @DisplayName("Should find all places")
    void shouldFindAllPlaces() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.findAll()).thenReturn(places);

        // When
        List<Place> result = placeService.findAll();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(savedPlace);
        verify(placeRepository).findAll();
    }

    @Test
    @DisplayName("Should find all visited places ordered by most recent visit")
    void shouldFindAllByMostRecentVisit() {
        // Given
        Place place1 = testPlace.withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        Place place2 = Place.create("Yellowstone", "Wyoming", "Wyoming", "USA",
                List.of(Visit.create(LocalDate.of(2024, 7, 1), 68.0, "Geysers", null)),
                false, 44.4280, -110.5885, null, null, null, null, null, null, new ArrayList<>())
                .withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        List<Place> places = Arrays.asList(place1, place2);
        when(placeRepository.findAllVisitedPlaces()).thenReturn(places);

        // When
        List<Place> result = placeService.findAllByMostRecentVisit();

        // Then
        assertThat(result).hasSize(2);
        // place2 should be first (July 1 is more recent than June 15)
        assertThat(result.get(0)).isEqualTo(place2);
        assertThat(result.get(1)).isEqualTo(place1);
        verify(placeRepository).findAllVisitedPlaces();
    }

    @Test
    @DisplayName("Should find all places ordered by name")
    void shouldFindAllByName() {
        // Given
        Place place1 = testPlace.withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        Place place2 = Place.create("Yellowstone", "Wyoming", "Wyoming", "USA",
                List.of(Visit.create(LocalDate.of(2024, 7, 1), 68.0, "Geysers", null)),
                false, 44.4280, -110.5885, null, null, null, null, null, null, new ArrayList<>())
                .withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        List<Place> places = Arrays.asList(place1, place2);
        when(placeRepository.findAllByOrderByNameAsc()).thenReturn(places);

        // When
        List<Place> result = placeService.findAllByName();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(place1);
        verify(placeRepository).findAllByOrderByNameAsc();
    }

    @Test
    @DisplayName("Should find place by id")
    void shouldFindPlaceById() {
        // Given
        String placeId = "place123";
        Place placeWithId = new Place(placeId, savedPlace.name(), savedPlace.location(),
                savedPlace.state(), savedPlace.country(), savedPlace.visits(),
                savedPlace.hasToilet(), savedPlace.latitude(), savedPlace.longitude(),
                savedPlace.formattedAddress(), savedPlace.googlePlaceId(), savedPlace.website(),
                savedPlace.phoneNumber(), savedPlace.googleRating(), savedPlace.googleReviewCount(),
                new ArrayList<>(), savedPlace.createdAt(), savedPlace.updatedAt(),
                null, null, null, null,
                false, "VISITED");
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(placeWithId));

        // When
        Optional<Place> result = placeService.findById(placeId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(placeId);
        verify(placeRepository).findById(placeId);
    }

    @Test
    @DisplayName("Should return empty when place not found by id")
    void shouldReturnEmptyWhenPlaceNotFound() {
        // Given
        when(placeRepository.findById(anyString())).thenReturn(Optional.empty());

        // When
        Optional<Place> result = placeService.findById("nonexistent");

        // Then
        assertThat(result).isEmpty();
        verify(placeRepository).findById("nonexistent");
    }

    @Test
    @DisplayName("Should create place with timestamps")
    void shouldCreatePlaceWithTimestamps() {
        // Given
        Place placeWithTimestamps = testPlace.withTimestamps(
                LocalDateTime.now(), LocalDateTime.now());
        when(placeRepository.save(any(Place.class))).thenReturn(placeWithTimestamps);

        // When
        Place result = placeService.create(testPlace);

        // Then
        assertThat(result.createdAt()).isNotNull();
        assertThat(result.updatedAt()).isNotNull();
        verify(placeRepository).save(any(Place.class));
    }

    @Test
    @DisplayName("Should update existing place")
    void shouldUpdateExistingPlace() {
        // Given
        String placeId = "place123";
        Place existingPlace = new Place(placeId, savedPlace.name(), savedPlace.location(),
                savedPlace.state(), savedPlace.country(), savedPlace.visits(),
                savedPlace.hasToilet(), savedPlace.latitude(), savedPlace.longitude(),
                savedPlace.formattedAddress(), savedPlace.googlePlaceId(), savedPlace.website(),
                savedPlace.phoneNumber(), savedPlace.googleRating(), savedPlace.googleReviewCount(),
                new ArrayList<>(),
                LocalDateTime.of(2024, 6, 15, 10, 0),
                LocalDateTime.of(2024, 6, 15, 10, 0),
                null, null, null, null,
                false, "VISITED");

        Place updatedData = Place.create("Updated Name", "Updated Location",
                "California", "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 20), 80.0, "Updated notes", null)),
                false, 37.8651, -119.5383, null, null, null, null, null, null, new ArrayList<>());

        when(placeRepository.findById(placeId)).thenReturn(Optional.of(existingPlace));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Place result = placeService.update(placeId, updatedData);

        // Then
        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.location()).isEqualTo("Updated Location");
        assertThat(result.createdAt()).isEqualTo(existingPlace.createdAt());
        assertThat(result.updatedAt()).isAfter(existingPlace.updatedAt());
        verify(placeRepository).findById(placeId);
        verify(placeRepository).save(any(Place.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent place")
    void shouldThrowExceptionWhenUpdatingNonExistentPlace() {
        // Given
        when(placeRepository.findById(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> placeService.update("nonexistent", testPlace))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place not found with id: nonexistent");
        verify(placeRepository).findById("nonexistent");
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    @DisplayName("Should delete place by id")
    void shouldDeletePlaceById() {
        // Given
        String placeId = "place123";
        Place placeToDelete = new Place(placeId, savedPlace.name(), savedPlace.location(),
                savedPlace.state(), savedPlace.country(), savedPlace.visits(),
                savedPlace.hasToilet(), savedPlace.latitude(), savedPlace.longitude(),
                savedPlace.formattedAddress(), savedPlace.googlePlaceId(), savedPlace.website(),
                savedPlace.phoneNumber(), savedPlace.googleRating(), savedPlace.googleReviewCount(),
                new ArrayList<>(),
                LocalDateTime.of(2024, 6, 15, 10, 0),
                LocalDateTime.of(2024, 6, 15, 10, 0),
                null, null, null, null,
                false, "VISITED");
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(placeToDelete));
        doNothing().when(placeRepository).deleteById(placeId);

        // When
        placeService.deleteById(placeId);

        // Then
        verify(placeRepository).findById(placeId);
        verify(placeRepository).deleteById(placeId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent place")
    void shouldThrowExceptionWhenDeletingNonExistentPlace() {
        // Given
        when(placeRepository.findById(anyString())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> placeService.deleteById("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Place not found with id: nonexistent");
        verify(placeRepository).findById("nonexistent");
        verify(placeRepository, never()).deleteById(anyString());
    }

    @Test
    @DisplayName("Should search places with valid search term")
    void shouldSearchPlacesWithValidSearchTerm() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.searchPlaces("Yosemite")).thenReturn(places);

        // When
        List<Place> result = placeService.search("Yosemite");

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).searchPlaces("Yosemite");
        verify(placeRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should return all places when search term is null")
    void shouldReturnAllPlacesWhenSearchTermIsNull() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.findAll()).thenReturn(places);

        // When
        List<Place> result = placeService.search(null);

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).findAll();
        verify(placeRepository, never()).searchPlaces(anyString());
    }

    @Test
    @DisplayName("Should return all places when search term is blank")
    void shouldReturnAllPlacesWhenSearchTermIsBlank() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.findAll()).thenReturn(places);

        // When
        List<Place> result = placeService.search("   ");

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).findAll();
        verify(placeRepository, never()).searchPlaces(anyString());
    }

    @Test
    @DisplayName("Should trim search term before searching")
    void shouldTrimSearchTermBeforeSearching() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.searchPlaces("Yosemite")).thenReturn(places);

        // When
        placeService.search("  Yosemite  ");

        // Then
        verify(placeRepository).searchPlaces("Yosemite");
    }

    @Test
    @DisplayName("Should find places by state")
    void shouldFindPlacesByState() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.findByStateIgnoreCase("California")).thenReturn(places);

        // When
        List<Place> result = placeService.findByState("California");

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).findByStateIgnoreCase("California");
    }

    @Test
    @DisplayName("Should find places by country")
    void shouldFindPlacesByCountry() {
        // Given
        List<Place> places = Arrays.asList(savedPlace);
        when(placeRepository.findByCountryIgnoreCase("USA")).thenReturn(places);

        // When
        List<Place> result = placeService.findByCountry("USA");

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).findByCountryIgnoreCase("USA");
    }

    @Test
    @DisplayName("Should count places by state")
    void shouldCountPlacesByState() {
        // Given
        when(placeRepository.countByStateIgnoreCase("California")).thenReturn(5L);

        // When
        long count = placeService.countByState("California");

        // Then
        assertThat(count).isEqualTo(5L);
        verify(placeRepository).countByStateIgnoreCase("California");
    }

    @Test
    @DisplayName("Should count places by country")
    void shouldCountPlacesByCountry() {
        // Given
        when(placeRepository.countByCountryIgnoreCase("USA")).thenReturn(10L);

        // When
        long count = placeService.countByCountry("USA");

        // Then
        assertThat(count).isEqualTo(10L);
        verify(placeRepository).countByCountryIgnoreCase("USA");
    }

    @Test
    @DisplayName("Should count all places")
    void shouldCountAllPlaces() {
        // Given
        when(placeRepository.count()).thenReturn(15L);

        // When
        long count = placeService.count();

        // Then
        assertThat(count).isEqualTo(15L);
        verify(placeRepository).count();
    }

    // ===== Favorites & Wishlist Tests =====

    @Test
    @DisplayName("Should find all visited places")
    void shouldFindAllVisited() {
        // Given
        when(placeRepository.findAllVisitedPlaces()).thenReturn(List.of(savedPlace));

        // When
        List<Place> result = placeService.findAllVisited();

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).findAllVisitedPlaces();
    }

    @Test
    @DisplayName("Should find all wishlist places")
    void shouldFindAllWishlist() {
        // Given
        when(placeRepository.findByStatus("TO_VISIT")).thenReturn(List.of());

        // When
        List<Place> result = placeService.findAllWishlist();

        // Then
        assertThat(result).isEmpty();
        verify(placeRepository).findByStatus("TO_VISIT");
    }

    @Test
    @DisplayName("Should find favorite places")
    void shouldFindFavorites() {
        // Given
        when(placeRepository.findFavoriteVisitedPlaces()).thenReturn(List.of(savedPlace));

        // When
        List<Place> result = placeService.findFavorites();

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).findFavoriteVisitedPlaces();
    }

    @Test
    @DisplayName("Should toggle favorite on a place")
    void shouldToggleFavorite() {
        // Given
        String placeId = "place123";
        Place placeWithId = new Place(placeId, savedPlace.name(), savedPlace.location(),
                savedPlace.state(), savedPlace.country(), savedPlace.visits(),
                savedPlace.hasToilet(), savedPlace.latitude(), savedPlace.longitude(),
                savedPlace.formattedAddress(), savedPlace.googlePlaceId(), savedPlace.website(),
                savedPlace.phoneNumber(), savedPlace.googleRating(), savedPlace.googleReviewCount(),
                new ArrayList<>(), savedPlace.createdAt(), savedPlace.updatedAt(),
                null, null, null, null,
                false, "VISITED");
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(placeWithId));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Place result = placeService.toggleFavorite(placeId);

        // Then
        assertThat(result.favorite()).isTrue();
        verify(placeRepository).save(any(Place.class));
    }

    @Test
    @DisplayName("Should convert wishlist item to visited")
    void shouldConvertToVisited() {
        // Given
        String placeId = "wish123";
        Place wishlistPlace = new Place(placeId, "Restaurant", "Downtown", "NY", "USA",
                new ArrayList<>(), false, 40.7, -74.0,
                null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null,
                false, "TO_VISIT");
        when(placeRepository.findById(placeId)).thenReturn(Optional.of(wishlistPlace));
        when(placeRepository.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));

        // When
        Place result = placeService.convertToVisited(placeId);

        // Then
        assertThat(result.status()).isEqualTo("VISITED");
        assertThat(result.isVisited()).isTrue();
        assertThat(result.isWishlist()).isFalse();
        verify(placeRepository).save(any(Place.class));
    }

    @Test
    @DisplayName("Should search visited places")
    void shouldSearchVisitedPlaces() {
        // Given
        when(placeRepository.searchVisitedPlaces("Yosemite")).thenReturn(List.of(savedPlace));

        // When
        List<Place> result = placeService.searchVisited("Yosemite");

        // Then
        assertThat(result).hasSize(1);
        verify(placeRepository).searchVisitedPlaces("Yosemite");
    }

    @Test
    @DisplayName("Should count wishlist places")
    void shouldCountWishlistPlaces() {
        // Given
        when(placeRepository.countWishlistPlaces()).thenReturn(5L);

        // When
        long count = placeService.countWishlist();

        // Then
        assertThat(count).isEqualTo(5L);
        verify(placeRepository).countWishlistPlaces();
    }

    @Test
    @DisplayName("Should count favorite places")
    void shouldCountFavoritePlaces() {
        // Given
        when(placeRepository.countByFavoriteTrue()).thenReturn(3L);

        // When
        long count = placeService.countFavorites();

        // Then
        assertThat(count).isEqualTo(3L);
        verify(placeRepository).countByFavoriteTrue();
    }
}
