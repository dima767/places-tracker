package dk.placestracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Places Tracker Application - Track visited places with comments, coordinates, and metadata.
 *
 * @author Dmitriy Kopylenko
 */
@SpringBootApplication
public class PlacesTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlacesTrackerApplication.class, args);
    }

}
