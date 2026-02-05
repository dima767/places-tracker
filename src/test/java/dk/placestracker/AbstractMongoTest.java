package dk.placestracker;

import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base test class for MongoDB integration tests using Testcontainers.
 * Provides isolated MongoDB instance for each test run.
 *
 * Uses @ServiceConnection (Spring Boot 3.1+) which automatically configures
 * MongoDB connection details from the Testcontainers instance, overriding
 * any properties from application.properties. No manual property configuration needed.
 */
@DataMongoTest
@Testcontainers
public abstract class AbstractMongoTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0");
}
