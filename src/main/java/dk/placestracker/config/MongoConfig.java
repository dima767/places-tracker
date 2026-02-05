package dk.placestracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration for Places Tracker.
 *
 * @author Dmitriy Kopylenko
 */
@Configuration
@EnableMongoRepositories(basePackages = "dk.placestracker.domain.repository")
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB configuration with auto-index creation enabled via application.properties
    // Indexes defined via @Indexed annotations will be created automatically
}
