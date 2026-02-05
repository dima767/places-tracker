package dk.placestracker.web.controller;

import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.Settings;
import dk.placestracker.domain.model.Visit;
import dk.placestracker.service.PlaceService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@DisplayName("HomeController Tests")
class HomeControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceService placeService;

    @MockitoBean
    private SettingsService settingsService;

    @BeforeEach
    void setUp() {
        // Mock settings service to return default settings (no home location)
        when(settingsService.getSettings()).thenReturn(Settings.createDefault());
    }

    @Test
    @DisplayName("Should display home page with place statistics")
    void shouldDisplayHomePageWithStatistics() throws Exception {
        // Given
        Place place1 = Place.create("Yosemite", "California", "California", "USA",
                List.of(Visit.create(LocalDate.of(2024, 6, 15), 75.0, "Beautiful", null)),
                false, 37.8651, -119.5383, null, null, null, null, null, null, new ArrayList<>())
                .withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        Place place2 = Place.create("Yellowstone", "Wyoming", "Wyoming", "USA",
                List.of(Visit.create(LocalDate.of(2024, 7, 1), 68.0, "Geysers", null)),
                false, 44.4280, -110.5885, null, null, null, null, null, null, new ArrayList<>())
                .withTimestamps(LocalDateTime.now(), LocalDateTime.now());

        List<Place> recentPlaces = Arrays.asList(place1, place2);

        when(placeService.count()).thenReturn(10L);
        when(placeService.findAllByMostRecentVisit()).thenReturn(recentPlaces);

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("totalPlaces"))
                .andExpect(model().attribute("totalPlaces", 10L))
                .andExpect(model().attributeExists("recentPlaces"))
                .andExpect(model().attribute("recentPlaces", hasSize(2)));

        verify(placeService).count();
        verify(placeService).findAllByMostRecentVisit();
    }

    @Test
    @DisplayName("Should limit recent places to 5")
    void shouldLimitRecentPlacesToFive() throws Exception {
        // Given
        List<Place> manyPlaces = Arrays.asList(
                createTestPlace("Place 1", LocalDate.of(2024, 1, 1)),
                createTestPlace("Place 2", LocalDate.of(2024, 2, 1)),
                createTestPlace("Place 3", LocalDate.of(2024, 3, 1)),
                createTestPlace("Place 4", LocalDate.of(2024, 4, 1)),
                createTestPlace("Place 5", LocalDate.of(2024, 5, 1)),
                createTestPlace("Place 6", LocalDate.of(2024, 6, 1)),
                createTestPlace("Place 7", LocalDate.of(2024, 7, 1))
        );

        when(placeService.count()).thenReturn(7L);
        when(placeService.findAllByMostRecentVisit()).thenReturn(manyPlaces);

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("recentPlaces", hasSize(6)));

        verify(placeService).count();
        verify(placeService).findAllByMostRecentVisit();
    }

    @Test
    @DisplayName("Should handle empty place list")
    void shouldHandleEmptyPlaceList() throws Exception {
        // Given
        when(placeService.count()).thenReturn(0L);
        when(placeService.findAllByMostRecentVisit()).thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("totalPlaces", 0L))
                .andExpect(model().attribute("recentPlaces", hasSize(0)));

        verify(placeService).count();
        verify(placeService).findAllByMostRecentVisit();
    }

    @Test
    @DisplayName("Should handle single place")
    void shouldHandleSinglePlace() throws Exception {
        // Given
        Place place = createTestPlace("Yosemite", LocalDate.of(2024, 6, 15));
        List<Place> singlePlace = Collections.singletonList(place);

        when(placeService.count()).thenReturn(1L);
        when(placeService.findAllByMostRecentVisit()).thenReturn(singlePlace);

        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("totalPlaces", 1L))
                .andExpect(model().attribute("recentPlaces", hasSize(1)));

        verify(placeService).count();
        verify(placeService).findAllByMostRecentVisit();
    }

    private Place createTestPlace(String name, LocalDate visitDate) {
        return Place.create(name, "Location", "State", "Country",
                List.of(Visit.create(visitDate, 70.0, "Notes", null)),
                false, 37.0, -119.0, null, null, null, null, null, null, new ArrayList<>())
                .withTimestamps(LocalDateTime.now(), LocalDateTime.now());
    }
}
