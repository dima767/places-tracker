package dk.placestracker.service;

import dk.placestracker.domain.model.Settings;
import dk.placestracker.domain.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultSettingsService Tests")
class DefaultSettingsServiceTests {

    @Mock
    private SettingsRepository settingsRepository;

    @Mock
    private DistanceService distanceService;

    private DefaultSettingsService settingsService;

    @BeforeEach
    void setUp() {
        settingsService = new DefaultSettingsService(settingsRepository, distanceService);
    }

    @Test
    @DisplayName("Should get existing settings")
    void shouldGetExistingSettings() {
        // Given
        Settings settings = Settings.createDefault().withHomeLocation(40.7128, -74.0060);
        when(settingsRepository.findById("default")).thenReturn(Optional.of(settings));

        // When
        Settings result = settingsService.getSettings();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.homeLatitude()).isEqualTo(40.7128);
        assertThat(result.homeLongitude()).isEqualTo(-74.0060);
        verify(settingsRepository).findById("default");
        verify(settingsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create default settings when none exist")
    void shouldCreateDefaultSettingsWhenNoneExist() {
        // Given
        Settings defaultSettings = Settings.createDefault();
        when(settingsRepository.findById("default")).thenReturn(Optional.empty());
        when(settingsRepository.save(any(Settings.class))).thenReturn(defaultSettings);

        // When
        Settings result = settingsService.getSettings();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.homeLatitude()).isNull();
        assertThat(result.homeLongitude()).isNull();
        verify(settingsRepository).findById("default");
        verify(settingsRepository).save(any(Settings.class));
    }

    @Test
    @DisplayName("Should update home location and invalidate distances")
    void shouldUpdateHomeLocation() {
        // Given
        Settings existingSettings = Settings.createDefault();
        Settings updatedSettings = existingSettings.withHomeLocation(50.0, -80.0);
        when(settingsRepository.findById("default")).thenReturn(Optional.of(existingSettings));
        when(settingsRepository.save(any(Settings.class))).thenReturn(updatedSettings);

        // When
        Settings result = settingsService.updateSettings(50.0, -80.0);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.homeLatitude()).isEqualTo(50.0);
        assertThat(result.homeLongitude()).isEqualTo(-80.0);
        verify(settingsRepository).findById("default");
        verify(settingsRepository).save(any(Settings.class));
        verify(distanceService).invalidateAllDistances();
    }

    @Test
    @DisplayName("Should clear home location and invalidate distances")
    void shouldClearHomeLocation() {
        // Given
        Settings existingSettings = Settings.createDefault().withHomeLocation(40.0, -70.0);
        Settings clearedSettings = existingSettings.withClearedHomeLocation();
        when(settingsRepository.findById("default")).thenReturn(Optional.of(existingSettings));
        when(settingsRepository.save(any(Settings.class))).thenReturn(clearedSettings);

        // When
        Settings result = settingsService.clearHomeLocation();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.homeLatitude()).isNull();
        assertThat(result.homeLongitude()).isNull();
        verify(settingsRepository).findById("default");
        verify(settingsRepository).save(any(Settings.class));
        verify(distanceService).invalidateAllDistances();
    }
}
