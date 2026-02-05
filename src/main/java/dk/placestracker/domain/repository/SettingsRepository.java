package dk.placestracker.domain.repository;

import dk.placestracker.domain.model.Settings;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * MongoDB repository for application settings.
 *
 * @author Dmitriy Kopylenko
 */
public interface SettingsRepository extends MongoRepository<Settings, String> {
}
