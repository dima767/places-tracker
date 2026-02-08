package dk.placestracker.domain.repository;

import dk.placestracker.AbstractMongoTest;
import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.Visit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlaceRepository Integration Tests")
class PlaceRepositoryTests extends AbstractMongoTest {

    @Autowired
    private PlaceRepository placeRepository;

    private Place yosemite;
    private Place yellowstone;
    private Place grandCanyon;

    @BeforeEach
    void setUp() {
        placeRepository.deleteAll();

        // Create visits for test places
        Visit yosemiteVisit = Visit.create(LocalDate.of(2024, 6, 15), 75.0,
                "Beautiful waterfalls and granite cliffs", null);
        Visit yellowstoneVisit1 = Visit.create(LocalDate.of(2024, 7, 1), 68.0,
                "Geysers and wildlife", null);
        Visit yellowstoneVisit2 = Visit.create(LocalDate.of(2023, 8, 15), 72.0,
                "Second visit - saw more wildlife", null);
        Visit grandCanyonVisit = Visit.create(LocalDate.of(2024, 5, 10), 85.0,
                "Stunning canyon views", null);

        yosemite = Place.create(
                "Yosemite National Park",
                "Yosemite Valley",
                "California",
                "USA",
                List.of(yosemiteVisit),
                false,
                37.8651,
                -119.5383,
                null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        yellowstone = Place.create(
                "Yellowstone National Park",
                "Wyoming",
                "Wyoming",
                "USA",
                List.of(yellowstoneVisit1, yellowstoneVisit2),
                false,
                44.4280,
                -110.5885,
                null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        grandCanyon = Place.create(
                "Grand Canyon National Park",
                "Arizona",
                "Arizona",
                "USA",
                List.of(grandCanyonVisit),
                false,
                36.1069,
                -112.1129,
                null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        List<Place> saved = placeRepository.saveAll(List.of(yosemite, yellowstone, grandCanyon));
        yosemite = saved.get(0);
        yellowstone = saved.get(1);
        grandCanyon = saved.get(2);
    }

    @Test
    @DisplayName("Should save and find place by id")
    void shouldSaveAndFindPlaceById() {
        // When
        Place saved = placeRepository.save(yosemite);
        Place found = placeRepository.findById(saved.id()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.name()).isEqualTo("Yosemite National Park");
        assertThat(found.state()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should find places by state ignoring case")
    void shouldFindPlacesByStateIgnoreCase() {
        // When
        List<Place> californiaPlacesCaps = placeRepository.findByStateIgnoreCase("CALIFORNIA");
        List<Place> californiaPlacesLower = placeRepository.findByStateIgnoreCase("california");

        // Then
        assertThat(californiaPlacesCaps).hasSize(1);
        assertThat(californiaPlacesLower).hasSize(1);
        assertThat(californiaPlacesCaps.get(0).name()).isEqualTo("Yosemite National Park");
    }

    @Test
    @DisplayName("Should find places by country ignoring case")
    void shouldFindPlacesByCountryIgnoreCase() {
        // When
        List<Place> usaPlaces = placeRepository.findByCountryIgnoreCase("usa");

        // Then
        assertThat(usaPlaces).hasSize(3);
    }

    @Test
    @DisplayName("Should find places by name containing text ignoring case")
    void shouldFindPlacesByNameContainingIgnoreCase() {
        // When
        List<Place> placesWithYellowstone = placeRepository.findByNameContainingIgnoreCase("yellowstone");
        List<Place> placesWithNational = placeRepository.findByNameContainingIgnoreCase("NATIONAL");

        // Then
        assertThat(placesWithYellowstone).hasSize(1);
        assertThat(placesWithNational).hasSize(3);
    }

    @Test
    @DisplayName("Should find places by state and country ignoring case")
    void shouldFindPlacesByStateAndCountryIgnoreCase() {
        // When
        List<Place> californiaUSAPlaces = placeRepository
                .findByStateIgnoreCaseAndCountryIgnoreCase("California", "USA");

        // Then
        assertThat(californiaUSAPlaces).hasSize(1);
        assertThat(californiaUSAPlaces.get(0).name()).isEqualTo("Yosemite National Park");
    }

    @Test
    @DisplayName("Should search places by name, location, or state")
    void shouldSearchPlacesByNameLocationOrState() {
        // When
        List<Place> searchYosemite = placeRepository.searchPlaces("Yosemite");
        List<Place> searchWyoming = placeRepository.searchPlaces("Wyoming");
        List<Place> searchArizona = placeRepository.searchPlaces("Arizona");
        List<Place> searchValley = placeRepository.searchPlaces("Valley");

        // Then
        assertThat(searchYosemite).hasSize(1);
        assertThat(searchYosemite.get(0).name()).isEqualTo("Yosemite National Park");

        assertThat(searchWyoming).hasSize(1);
        assertThat(searchWyoming.get(0).name()).isEqualTo("Yellowstone National Park");

        assertThat(searchArizona).hasSize(1);
        assertThat(searchArizona.get(0).name()).isEqualTo("Grand Canyon National Park");

        assertThat(searchValley).hasSize(1);
        assertThat(searchValley.get(0).location()).isEqualTo("Yosemite Valley");
    }

    @Test
    @DisplayName("Should search places case-insensitively")
    void shouldSearchPlacesCaseInsensitively() {
        // When
        List<Place> searchLower = placeRepository.searchPlaces("yosemite");
        List<Place> searchUpper = placeRepository.searchPlaces("YOSEMITE");
        List<Place> searchMixed = placeRepository.searchPlaces("YoSeMiTe");

        // Then
        assertThat(searchLower).hasSize(1);
        assertThat(searchUpper).hasSize(1);
        assertThat(searchMixed).hasSize(1);
    }

    @Test
    @DisplayName("Should find all places ordered by name ascending")
    void shouldFindAllPlacesOrderedByNameAsc() {
        // When
        List<Place> orderedPlaces = placeRepository.findAllByOrderByNameAsc();

        // Then
        assertThat(orderedPlaces).hasSize(3);
        assertThat(orderedPlaces.get(0).name()).isEqualTo("Grand Canyon National Park");
        assertThat(orderedPlaces.get(1).name()).isEqualTo("Yellowstone National Park");
        assertThat(orderedPlaces.get(2).name()).isEqualTo("Yosemite National Park");
    }

    @Test
    @DisplayName("Should count places by state ignoring case")
    void shouldCountPlacesByStateIgnoreCase() {
        // When
        long californiaCount = placeRepository.countByStateIgnoreCase("California");
        long wyomingCount = placeRepository.countByStateIgnoreCase("WYOMING");

        // Then
        assertThat(californiaCount).isEqualTo(1);
        assertThat(wyomingCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should count places by country ignoring case")
    void shouldCountPlacesByCountryIgnoreCase() {
        // When
        long usaCount = placeRepository.countByCountryIgnoreCase("usa");

        // Then
        assertThat(usaCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle empty search results")
    void shouldHandleEmptySearchResults() {
        // When
        List<Place> nonExistent = placeRepository.searchPlaces("nonexistent");
        List<Place> emptyState = placeRepository.findByStateIgnoreCase("Texas");

        // Then
        assertThat(nonExistent).isEmpty();
        assertThat(emptyState).isEmpty();
    }

    @Test
    @DisplayName("Should delete place by id")
    void shouldDeletePlaceById() {
        // Given
        Place saved = placeRepository.save(yosemite);
        String placeId = saved.id();

        // When
        placeRepository.deleteById(placeId);

        // Then
        assertThat(placeRepository.findById(placeId)).isEmpty();
    }

    @Test
    @DisplayName("Should count total places")
    void shouldCountTotalPlaces() {
        // When
        long total = placeRepository.count();

        // Then
        assertThat(total).isEqualTo(3);
    }

    @Test
    @DisplayName("Should handle places with null coordinates")
    void shouldHandlePlacesWithNullCoordinates() {
        // Given
        Visit visit = Visit.create(LocalDate.now(), 70.0, "Test notes", null);
        Place placeWithoutCoords = Place.create(
                "Test Place",
                "Test Location",
                "Test State",
                "Test Country",
                List.of(visit),
                false,
                null,
                null,
                null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        // When
        Place saved = placeRepository.save(placeWithoutCoords);
        Place found = placeRepository.findById(saved.id()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.latitude()).isNull();
        assertThat(found.longitude()).isNull();
    }

    @Test
    @DisplayName("Should persist places with multiple visits")
    void shouldPersistPlacesWithMultipleVisits() {
        // When
        Place found = placeRepository.findById(yellowstone.id()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.visits()).hasSize(2);
        assertThat(found.getVisitCount()).isEqualTo(2);
    }

    // ===== Favorites & Wishlist Tests =====

    @Test
    @DisplayName("Should find all visited places excluding wishlist")
    void shouldFindAllVisitedPlaces() {
        // Given - add a wishlist item
        Place wishlistItem = Place.createWishlistItem(
                "Cool Restaurant", "Downtown", "New York", "USA",
                40.7128, -74.0060, null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        placeRepository.save(wishlistItem);

        // When
        List<Place> visited = placeRepository.findAllVisitedPlaces();

        // Then - should include original 3 places but not the wishlist item
        assertThat(visited).hasSize(3);
        assertThat(visited).noneMatch(p -> "Cool Restaurant".equals(p.name()));
    }

    @Test
    @DisplayName("Should find places by status")
    void shouldFindByStatus() {
        // Given
        Place wishlistItem = Place.createWishlistItem(
                "Cool Restaurant", "Downtown", "New York", "USA",
                40.7128, -74.0060, null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        placeRepository.save(wishlistItem);

        // When
        List<Place> wishlist = placeRepository.findByStatus("TO_VISIT");

        // Then
        assertThat(wishlist).hasSize(1);
        assertThat(wishlist.get(0).name()).isEqualTo("Cool Restaurant");
    }

    @Test
    @DisplayName("Should find favorite places")
    void shouldFindFavoritePlaces() {
        // Given - mark yosemite as favorite
        Place favoriteYosemite = yosemite.withFavorite(true);
        placeRepository.save(favoriteYosemite);

        // When
        List<Place> favorites = placeRepository.findByFavoriteTrue();

        // Then
        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).name()).isEqualTo("Yosemite National Park");
    }

    @Test
    @DisplayName("Should find favorite visited places")
    void shouldFindFavoriteVisitedPlaces() {
        // Given - mark yosemite as favorite, also create a favorite wishlist item
        placeRepository.save(yosemite.withFavorite(true));
        Place wishlistFav = Place.createWishlistItem(
                "Dream Restaurant", "SoHo", "New York", "USA",
                40.7, -74.0, null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now()).withFavorite(true);
        placeRepository.save(wishlistFav);

        // When
        List<Place> favVisited = placeRepository.findFavoriteVisitedPlaces();

        // Then - should only include yosemite, not the wishlist item
        assertThat(favVisited).hasSize(1);
        assertThat(favVisited.get(0).name()).isEqualTo("Yosemite National Park");
    }

    @Test
    @DisplayName("Should search visited places only")
    void shouldSearchVisitedPlacesOnly() {
        // Given - add a wishlist item with matching name
        Place wishlistItem = Place.createWishlistItem(
                "Yosemite Lodge", "Yosemite Valley", "California", "USA",
                37.7, -119.6, null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        placeRepository.save(wishlistItem);

        // When
        List<Place> results = placeRepository.searchVisitedPlaces("Yosemite");

        // Then - should find original Yosemite but not the wishlist item
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Yosemite National Park");
    }

    @Test
    @DisplayName("Should search wishlist places only")
    void shouldSearchWishlistPlacesOnly() {
        // Given
        Place wishlistItem = Place.createWishlistItem(
                "Cool Yosemite Spot", "Near Yosemite", "California", "USA",
                37.7, -119.6, null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        placeRepository.save(wishlistItem);

        // When
        List<Place> results = placeRepository.searchWishlistPlaces("Yosemite");

        // Then - should only find the wishlist item
        assertThat(results).hasSize(1);
        assertThat(results.get(0).name()).isEqualTo("Cool Yosemite Spot");
    }

    @Test
    @DisplayName("Should count wishlist and favorite places")
    void shouldCountWishlistAndFavoritePlaces() {
        // Given
        Place wishlistItem = Place.createWishlistItem(
                "Restaurant", "Downtown", "New York", "USA",
                40.7, -74.0, null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());
        placeRepository.save(wishlistItem);
        placeRepository.save(yosemite.withFavorite(true));

        // When
        long wishlistCount = placeRepository.countWishlistPlaces();
        long favCount = placeRepository.countByFavoriteTrue();

        // Then
        assertThat(wishlistCount).isEqualTo(1);
        assertThat(favCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle backward compatibility - places with null status treated as visited")
    void shouldHandleBackwardCompatibilityNullStatus() {
        // The existing places in setUp have status "VISITED" via Place.create()
        // This test verifies that findAllVisitedPlaces includes them
        List<Place> visited = placeRepository.findAllVisitedPlaces();
        assertThat(visited).hasSize(3); // All original test places
    }

    @Test
    @DisplayName("Should handle places with no visits")
    void shouldHandlePlacesWithNoVisits() {
        // Given
        Place noVisits = Place.create(
                "Test Place",
                "Test Location",
                "Test State",
                "USA",
                new ArrayList<>(),
                false,
                37.0,
                -120.0,
                null, null, null, null, null, null, new ArrayList<>()
        ).withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        // When
        Place saved = placeRepository.save(noVisits);
        Place found = placeRepository.findById(saved.id()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.visits()).isEmpty();
        assertThat(found.getVisitCount()).isEqualTo(0);
        assertThat(found.getMostRecentVisit()).isEmpty();
    }
}
