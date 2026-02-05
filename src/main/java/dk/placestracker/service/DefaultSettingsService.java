package dk.placestracker.service;

import dk.placestracker.domain.model.Settings;
import dk.placestracker.domain.repository.SettingsRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of SettingsService.
 *
 * @author Dmitriy Kopylenko
 */
@Service
@Transactional
public class DefaultSettingsService implements SettingsService {

    private final SettingsRepository settingsRepository;
    private final DistanceService distanceService;

    public DefaultSettingsService(SettingsRepository settingsRepository,
                                   @Lazy DistanceService distanceService) {
        this.settingsRepository = settingsRepository;
        this.distanceService = distanceService;
    }

    @Override
    public Settings getSettings() {
        return settingsRepository.findById("default")
                .orElseGet(() -> settingsRepository.save(Settings.createDefault()));
    }

    @Override
    public Settings updateSettings(Double latitude, Double longitude) {
        Settings current = getSettings();
        Settings updated = current.withHomeLocation(latitude, longitude);
        Settings saved = settingsRepository.save(updated);

        // Invalidate all cached driving distances when home location changes
        distanceService.invalidateAllDistances();

        return saved;
    }

    @Override
    public Settings clearHomeLocation() {
        Settings current = getSettings();
        Settings updated = current.withClearedHomeLocation();
        Settings saved = settingsRepository.save(updated);

        // Invalidate all cached driving distances when home location is cleared
        distanceService.invalidateAllDistances();

        return saved;
    }
}
