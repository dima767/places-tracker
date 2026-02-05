package dk.placestracker.service;

import dk.placestracker.domain.model.Place;

import java.util.List;
import java.util.Map;

/**
 * Service for calculating driving distances from home location to places.
 * Uses Google Distance Matrix API with caching and Haversine fallback.
 *
 * @author Dmitriy Kopylenko
 */
public interface DistanceService {

    /**
     * Get the driving distance from home to a single place.
     * Uses cached value if home location hasn't changed, otherwise calls API.
     *
     * @param place the place to calculate distance to
     * @return distance result with miles, duration, and source
     */
    DistanceResult getDistance(Place place);

    /**
     * Get driving distances from home to multiple places.
     * Batches API calls (up to 25 destinations per request) for efficiency.
     *
     * @param places list of places to calculate distances to
     * @return map of place ID to distance result
     */
    Map<String, DistanceResult> getDistances(List<Place> places);

    /**
     * Invalidate all cached distances.
     * Should be called when home location changes.
     */
    void invalidateAllDistances();
}
