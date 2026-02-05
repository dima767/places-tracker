package dk.placestracker.web.controller;

import dk.placestracker.domain.model.Settings;
import dk.placestracker.service.SettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SettingsController.class)
@DisplayName("SettingsController Tests")
class SettingsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    @DisplayName("Should display settings page with existing settings")
    void shouldDisplaySettingsPage() throws Exception {
        // Given
        Settings settings = Settings.createDefault().withHomeLocation(40.7128, -74.0060);
        when(settingsService.getSettings()).thenReturn(settings);

        // When/Then
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/index"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(model().attribute("settings", settings));

        verify(settingsService).getSettings();
    }

    @Test
    @DisplayName("Should update home location with valid coordinates")
    void shouldUpdateHomeLocation() throws Exception {
        // Given
        Settings existingSettings = Settings.createDefault();
        Settings updatedSettings = existingSettings.withHomeLocation(50.0, -80.0);
        when(settingsService.updateSettings(50.0, -80.0)).thenReturn(updatedSettings);

        // When/Then
        mockMvc.perform(post("/settings")
                        .param("homeLatitude", "50.0")
                        .param("homeLongitude", "-80.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"))
                .andExpect(flash().attributeExists("success"));

        verify(settingsService).updateSettings(50.0, -80.0);
    }

    @Test
    @DisplayName("Should reject invalid coordinates")
    void shouldRejectInvalidCoordinates() throws Exception {
        // Given
        Settings settings = Settings.createDefault();
        when(settingsService.getSettings()).thenReturn(settings);

        // When/Then - invalid latitude (> 90)
        mockMvc.perform(post("/settings")
                        .param("homeLatitude", "95.0")
                        .param("homeLongitude", "-80.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/index"))
                .andExpect(model().attributeExists("settings"))
                .andExpect(model().hasErrors());

        verify(settingsService, never()).updateSettings(anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should clear home location")
    void shouldClearHomeLocation() throws Exception {
        // Given
        Settings clearedSettings = Settings.createDefault();
        when(settingsService.clearHomeLocation()).thenReturn(clearedSettings);

        // When/Then
        mockMvc.perform(post("/settings/clear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"))
                .andExpect(flash().attributeExists("success"));

        verify(settingsService).clearHomeLocation();
    }
}
