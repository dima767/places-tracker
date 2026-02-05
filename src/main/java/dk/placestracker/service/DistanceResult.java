package dk.placestracker.service;

/**
 * Result of a distance calculation containing miles, duration, and source information.
 *
 * @author Dmitriy Kopylenko
 */
public record DistanceResult(
        Double miles,
        Double durationMinutes,
        Source source
) {
    /**
     * Source of the distance calculation.
     */
    public enum Source {
        /** Distance retrieved from cache (home location unchanged) */
        CACHED,
        /** Distance calculated via Google Distance Matrix API */
        API,
        /** Fallback to Haversine formula (straight-line distance) */
        FALLBACK,
        /** Distance unavailable (no home location or missing coordinates) */
        UNAVAILABLE
    }

    /**
     * Create a result from cached data.
     */
    public static DistanceResult cached(Double miles, Double durationMinutes) {
        return new DistanceResult(miles, durationMinutes, Source.CACHED);
    }

    /**
     * Create a result from API response.
     */
    public static DistanceResult fromApi(Double miles, Double durationMinutes) {
        return new DistanceResult(miles, durationMinutes, Source.API);
    }

    /**
     * Create a fallback result using Haversine distance (no duration available).
     */
    public static DistanceResult fallback(Double miles) {
        return new DistanceResult(miles, null, Source.FALLBACK);
    }

    /**
     * Create an unavailable result.
     */
    public static DistanceResult unavailable() {
        return new DistanceResult(null, null, Source.UNAVAILABLE);
    }

    /**
     * Check if this result has valid distance data.
     */
    public boolean hasDistance() {
        return miles != null;
    }

    /**
     * Check if this result has duration data.
     */
    public boolean hasDuration() {
        return durationMinutes != null;
    }

    /**
     * Check if this is a fallback (approximate) result.
     */
    public boolean isFallback() {
        return source == Source.FALLBACK;
    }
}
