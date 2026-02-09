package dk.placestracker.web.controller;

import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.Settings;
import dk.placestracker.domain.model.Visit;
import dk.placestracker.service.DistanceResult;
import dk.placestracker.service.DistanceService;
import dk.placestracker.service.GoogleMapsService;
import dk.placestracker.service.PlaceService;
import dk.placestracker.service.PhotoService;
import dk.placestracker.service.ReferenceDataService;
import dk.placestracker.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaceController.class)
@DisplayName("PlaceController Tests")
class PlaceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private ReferenceDataService referenceDataService;

    @MockitoBean
    private GoogleMapsService googleMapsService;

    @MockitoBean
    private PhotoService photoService;

    @MockitoBean
    private SettingsService settingsService;

    @MockitoBean
    private DistanceService distanceService;

    private Place testPlace;

    @BeforeEach
    void setUp() {
        testPlace = new Place(
                "place123",
                "Yosemite National Park",
                "Yosemite Valley",
                "California",
                "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 15), 75.0, "Beautiful waterfalls and granite cliffs", null)),
                false,
                37.8651,
                -119.5383,
                null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.of(2024, 6, 15, 10, 0),
                LocalDateTime.of(2024, 6, 15, 10, 0),
                null, null, null, null,
                false, "VISITED"
        );

        // Mock reference data service to return empty lists
        when(referenceDataService.getAllCountries()).thenReturn(Collections.emptyList());
        when(referenceDataService.getAllStatesProvinces()).thenReturn(Collections.emptyList());

        // Mock settings service to return default settings (no home location)
        when(settingsService.getSettings()).thenReturn(Settings.createDefault());

        // Mock distance service to return unavailable by default (no home location)
        when(distanceService.getDistance(any(Place.class))).thenReturn(DistanceResult.unavailable());
        when(distanceService.getDistances(anyList())).thenReturn(Collections.emptyMap());
    }

    @Test
    @DisplayName("Should list all visited places with pagination")
    void shouldListAllPlaces() throws Exception {
        // Given
        List<Place> places = Arrays.asList(testPlace);
        when(placeService.findAllVisited()).thenReturn(places);

        // When/Then
        mockMvc.perform(get("/places"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list"))
                .andExpect(model().attributeExists("places"))
                .andExpect(model().attributeExists("placePage"))
                .andExpect(model().attribute("places", hasSize(1)));

        verify(placeService).findAllVisited();
    }

    @Test
    @DisplayName("Should view place details")
    void shouldViewPlaceDetails() throws Exception {
        // Given
        when(placeService.findById("place123")).thenReturn(Optional.of(testPlace));

        // When/Then
        mockMvc.perform(get("/places/{id}", "place123"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/detail"))
                .andExpect(model().attributeExists("place"))
                .andExpect(model().attribute("place", testPlace));

        verify(placeService).findById("place123");
    }

    @Test
    @DisplayName("Should redirect when place not found")
    void shouldRedirectWhenPlaceNotFound() throws Exception {
        // Given
        when(placeService.findById(anyString())).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/places/{id}", "nonexistent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places"))
                .andExpect(flash().attribute("error", "Place not found"));

        verify(placeService).findById("nonexistent");
    }

    @Test
    @DisplayName("Should show create place form")
    void shouldShowCreatePlaceForm() throws Exception {
        // When/Then
        mockMvc.perform(get("/places/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/create"))
                .andExpect(model().attributeExists("place"));
    }

    @Test
    @DisplayName("Should create place successfully")
    void shouldCreatePlaceSuccessfully() throws Exception {
        // Given
        when(placeService.create(any(Place.class))).thenReturn(testPlace);

        // When/Then
        mockMvc.perform(post("/places")
                        .param("name", "Yosemite National Park")
                        .param("location", "Yosemite Valley")
                        .param("state", "California")
                        .param("country", "USA")
                        .param("hasToilet", "false")
                        .param("favorite", "false")
                        .param("status", "VISITED")
                        .param("visitDate", "2024-06-15")
                        .param("notes", "Beautiful waterfalls and granite cliffs")
                        .param("latitude", "37.8651")
                        .param("longitude", "-119.5383"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places/place123"))
                .andExpect(flash().attribute("success", "Place created successfully!"));

        verify(placeService).create(any(Place.class));
    }

    @Test
    @DisplayName("Should handle validation errors when creating place")
    void shouldHandleValidationErrorsWhenCreating() throws Exception {
        // When/Then - Missing required fields
        mockMvc.perform(post("/places")
                        .param("name", "")
                        .param("location", "")
                        .param("state", "")
                        .param("country", "")
                        .param("hasToilet", "false")
                        .param("favorite", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/create"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("place", "name"))
                .andExpect(model().attributeHasFieldErrors("place", "location"))
                .andExpect(model().attributeHasFieldErrors("place", "state"))
                .andExpect(model().attributeHasFieldErrors("place", "country"));

        verify(placeService, never()).create(any(Place.class));
    }

    @Test
    @DisplayName("Should handle exception when creating place")
    void shouldHandleExceptionWhenCreating() throws Exception {
        // Given
        when(placeService.create(any(Place.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When/Then
        mockMvc.perform(post("/places")
                        .param("name", "Yosemite National Park")
                        .param("location", "Yosemite Valley")
                        .param("state", "California")
                        .param("country", "USA")
                        .param("hasToilet", "false")
                        .param("favorite", "false")
                        .param("visitDate", "2024-06-15"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/create"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", containsString("Error creating place")));
    }

    @Test
    @DisplayName("Should show edit place form")
    void shouldShowEditPlaceForm() throws Exception {
        // Given
        when(placeService.findById("place123")).thenReturn(Optional.of(testPlace));

        // When/Then
        mockMvc.perform(get("/places/{id}/edit", "place123"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/edit"))
                .andExpect(model().attributeExists("place"))
                .andExpect(model().attribute("place", testPlace));

        verify(placeService).findById("place123");
    }

    @Test
    @DisplayName("Should redirect when editing non-existent place")
    void shouldRedirectWhenEditingNonExistentPlace() throws Exception {
        // Given
        when(placeService.findById(anyString())).thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/places/{id}/edit", "nonexistent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places"))
                .andExpect(flash().attribute("error", "Place not found"));

        verify(placeService).findById("nonexistent");
    }

    @Test
    @DisplayName("Should update place successfully")
    void shouldUpdatePlaceSuccessfully() throws Exception {
        // Given
        when(placeService.update(eq("place123"), any(Place.class))).thenReturn(testPlace);

        // When/Then
        mockMvc.perform(post("/places/{id}", "place123")
                        .param("name", "Updated Yosemite")
                        .param("location", "Updated Location")
                        .param("state", "California")
                        .param("country", "USA")
                        .param("hasToilet", "false")
                        .param("favorite", "false")
                        .param("status", "VISITED")
                        .param("visitDate", "2024-06-15")
                        .param("notes", "Updated notes")
                        .param("latitude", "37.8651")
                        .param("longitude", "-119.5383"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places/place123"))
                .andExpect(flash().attribute("success", "Place updated successfully!"));

        verify(placeService).update(eq("place123"), any(Place.class));
    }

    @Test
    @DisplayName("Should handle validation errors when updating place")
    void shouldHandleValidationErrorsWhenUpdating() throws Exception {
        // When/Then
        mockMvc.perform(post("/places/{id}", "place123")
                        .param("name", "")
                        .param("location", "")
                        .param("state", "")
                        .param("country", "")
                        .param("hasToilet", "false")
                        .param("favorite", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/edit"))
                .andExpect(model().hasErrors());

        verify(placeService, never()).update(anyString(), any(Place.class));
    }

    @Test
    @DisplayName("Should handle place not found when updating")
    void shouldHandlePlaceNotFoundWhenUpdating() throws Exception {
        // Given
        when(placeService.update(eq("nonexistent"), any(Place.class)))
                .thenThrow(new IllegalArgumentException("Place not found"));

        // When/Then
        mockMvc.perform(post("/places/{id}", "nonexistent")
                        .param("name", "Test Place")
                        .param("location", "Test Location")
                        .param("state", "Test State")
                        .param("country", "Test Country")
                        .param("hasToilet", "false")
                        .param("favorite", "false")
                        .param("visitDate", "2024-06-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places"))
                .andExpect(flash().attribute("error", "Place not found"));
    }

    @Test
    @DisplayName("Should handle exception when updating place")
    void shouldHandleExceptionWhenUpdating() throws Exception {
        // Given
        when(placeService.update(eq("place123"), any(Place.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When/Then
        mockMvc.perform(post("/places/{id}", "place123")
                        .param("name", "Test Place")
                        .param("location", "Test Location")
                        .param("state", "Test State")
                        .param("country", "Test Country")
                        .param("hasToilet", "false")
                        .param("favorite", "false")
                        .param("visitDate", "2024-06-15"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/edit"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", containsString("Error updating place")));
    }

    @Test
    @DisplayName("Should delete place successfully")
    void shouldDeletePlaceSuccessfully() throws Exception {
        // Given
        doNothing().when(placeService).deleteById("place123");

        // When/Then
        mockMvc.perform(post("/places/{id}/delete", "place123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places"))
                .andExpect(flash().attribute("success", "Place deleted successfully!"));

        verify(placeService).deleteById("place123");
    }

    @Test
    @DisplayName("Should handle place not found when deleting")
    void shouldHandlePlaceNotFoundWhenDeleting() throws Exception {
        // Given
        doThrow(new IllegalArgumentException("Place not found"))
                .when(placeService).deleteById("nonexistent");

        // When/Then
        mockMvc.perform(post("/places/{id}/delete", "nonexistent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places"))
                .andExpect(flash().attribute("error", "Place not found"));

        verify(placeService).deleteById("nonexistent");
    }

    @Test
    @DisplayName("Should handle exception when deleting place")
    void shouldHandleExceptionWhenDeleting() throws Exception {
        // Given
        doThrow(new RuntimeException("Database error"))
                .when(placeService).deleteById("place123");

        // When/Then
        mockMvc.perform(post("/places/{id}/delete", "place123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places"))
                .andExpect(flash().attribute("error", containsString("Error deleting place")));

        verify(placeService).deleteById("place123");
    }

    @Test
    @DisplayName("Should search visited places with query parameter - htmx endpoint")
    void shouldSearchPlacesWithQuery() throws Exception {
        // Given
        List<Place> searchResults = Arrays.asList(testPlace);
        when(placeService.searchVisited("Yosemite")).thenReturn(searchResults);

        // When/Then
        mockMvc.perform(get("/places/search")
                        .param("q", "Yosemite"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list :: places-table"))
                .andExpect(model().attributeExists("places"))
                .andExpect(model().attribute("places", hasSize(1)))
                .andExpect(model().attribute("places", searchResults));

        verify(placeService).searchVisited("Yosemite");
    }

    @Test
    @DisplayName("Should return all visited places when search query is empty - htmx endpoint")
    void shouldReturnAllPlacesWhenSearchQueryIsEmpty() throws Exception {
        // Given
        List<Place> allPlaces = Arrays.asList(testPlace);
        when(placeService.findAllVisited()).thenReturn(allPlaces);

        // When/Then
        mockMvc.perform(get("/places/search")
                        .param("q", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list :: places-table"))
                .andExpect(model().attributeExists("places"))
                .andExpect(model().attributeExists("placePage"))
                .andExpect(model().attribute("places", hasSize(1)));

        verify(placeService).findAllVisited();
        verify(placeService, never()).searchVisited(anyString());
    }

    @Test
    @DisplayName("Should return all visited places when no query parameter provided - htmx endpoint")
    void shouldReturnAllPlacesWhenNoQueryParameter() throws Exception {
        // Given
        List<Place> allPlaces = Arrays.asList(testPlace);
        when(placeService.findAllVisited()).thenReturn(allPlaces);

        // When/Then
        mockMvc.perform(get("/places/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list :: places-table"))
                .andExpect(model().attributeExists("places"))
                .andExpect(model().attributeExists("placePage"))
                .andExpect(model().attribute("places", hasSize(1)));

        verify(placeService).findAllVisited();
        verify(placeService, never()).searchVisited(anyString());
    }

    @Test
    @DisplayName("Should handle empty search results - htmx endpoint")
    void shouldHandleEmptySearchResults() throws Exception {
        // Given
        when(placeService.searchVisited("nonexistent")).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/places/search")
                        .param("q", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list :: places-table"))
                .andExpect(model().attributeExists("places"))
                .andExpect(model().attribute("places", hasSize(0)));

        verify(placeService).searchVisited("nonexistent");
    }

    @Test
    @DisplayName("Should search with partial match - htmx endpoint")
    void shouldSearchWithPartialMatch() throws Exception {
        // Given
        Place place1 = new Place("id1", "Yosemite National Park", "Location1", "California", "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 15), 75.0, "Notes", null)),
                false, 37.0, -119.0, null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, null,
                false, "VISITED");
        Place place2 = new Place("id2", "Yosemite Valley", "Location2", "California", "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 16), 68.0, "Notes", null)),
                false, 37.0, -119.0, null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, null,
                false, "VISITED");

        List<Place> searchResults = Arrays.asList(place1, place2);
        when(placeService.searchVisited("Yosemite")).thenReturn(searchResults);

        // When/Then
        mockMvc.perform(get("/places/search")
                        .param("q", "Yosemite"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list :: places-table"))
                .andExpect(model().attributeExists("placePage"))
                .andExpect(model().attribute("places", hasSize(2)));

        verify(placeService).searchVisited("Yosemite");
    }

    // ===== Favorites & Wishlist Tests =====

    @Test
    @DisplayName("Should toggle favorite via htmx")
    void shouldToggleFavorite() throws Exception {
        // Given
        Place favorited = new Place(
                "place123", "Yosemite National Park", "Yosemite Valley", "California", "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 15), 75.0, "Notes", null)),
                false, 37.8651, -119.5383, null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, null,
                true, "VISITED"
        );
        when(placeService.toggleFavorite("place123")).thenReturn(favorited);

        // When/Then
        mockMvc.perform(post("/places/place123/toggle-favorite")
                        .header("HX-Request", "true"))
                .andExpect(status().isOk());

        verify(placeService).toggleFavorite("place123");
    }

    @Test
    @DisplayName("Should list favorites")
    void shouldListFavorites() throws Exception {
        // Given
        when(placeService.findFavorites()).thenReturn(Arrays.asList(testPlace));

        // When/Then
        mockMvc.perform(get("/places/favorites"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/list"))
                .andExpect(model().attribute("listType", "favorites"));

        verify(placeService).findFavorites();
    }

    @Test
    @DisplayName("Should list wishlist")
    void shouldListWishlist() throws Exception {
        // Given
        when(placeService.findAllWishlist()).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/places/wishlist"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/wishlist-list"))
                .andExpect(model().attribute("listType", "wishlist"));

        verify(placeService).findAllWishlist();
    }

    @Test
    @DisplayName("Should show wishlist create form")
    void shouldShowWishlistCreateForm() throws Exception {
        // When/Then
        mockMvc.perform(get("/places/wishlist/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/wishlist-create"))
                .andExpect(model().attributeExists("place"));
    }

    @Test
    @DisplayName("Should create wishlist item successfully")
    void shouldCreateWishlistItemSuccessfully() throws Exception {
        // Given
        Place wishlistItem = new Place(
                "wish123", "New Restaurant", "Downtown", "New York", "USA",
                new ArrayList<>(), false, 40.7, -74.0, null, null, null, null, null, null, new ArrayList<>(),
                LocalDateTime.now(), LocalDateTime.now(), null, null, null, null,
                false, "TO_VISIT"
        );
        when(placeService.create(any(Place.class))).thenReturn(wishlistItem);

        // When/Then
        mockMvc.perform(post("/places/wishlist")
                        .param("name", "New Restaurant")
                        .param("location", "Downtown")
                        .param("state", "New York")
                        .param("country", "USA")
                        .param("favorite", "false")
                        .param("hasToilet", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places/wish123"));

        verify(placeService).create(any(Place.class));
    }

    @Test
    @DisplayName("Should convert wishlist item to visited")
    void shouldConvertToVisited() throws Exception {
        // Given
        when(placeService.findById("place123")).thenReturn(Optional.of(testPlace));

        // When/Then
        mockMvc.perform(post("/places/place123/convert-to-visited"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/places/place123/edit?convertToVisited=true"))
                .andExpect(flash().attribute("success", containsString("save to mark as visited")));

        verify(placeService, never()).convertToVisited(any());
    }

    @Test
    @DisplayName("Should search wishlist")
    void shouldSearchWishlist() throws Exception {
        // Given
        when(placeService.searchWishlist("restaurant")).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/places/wishlist/search")
                        .param("q", "restaurant"))
                .andExpect(status().isOk())
                .andExpect(view().name("places/wishlist-list :: places-table"));

        verify(placeService).searchWishlist("restaurant");
    }
}
