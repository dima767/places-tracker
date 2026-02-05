package dk.placestracker.service;

import dk.placestracker.domain.model.Settings;

/**
 * Service for managing application settings.
 *
 * @author Dmitriy Kopylenko
 */
public interface SettingsService {

    /**
     * Gets current settings, creating default if none exist.
     *
     * @return Current settings (never null)
     */
    Settings getSettings();

    /**
     * Updates home location coordinates.
     *
     * @param latitude  Home latitude
     * @param longitude Home longitude
     * @return Updated settings
     */
    Settings updateSettings(Double latitude, Double longitude);

    /**
     * Clears home location coordinates.
     *
     * @return Updated settings with null coordinates
     */
    Settings clearHomeLocation();
}
