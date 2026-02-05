package dk.placestracker.util;

import java.text.DecimalFormat;

/**
 * Utility class for calculating distances between geographic coordinates using the Haversine formula.
 *
 * @author Dmitriy Kopylenko
 */
public final class DistanceCalculator {

    private static final double EARTH_RADIUS_MILES = 3958.8;
    private static final DecimalFormat DISTANCE_FORMAT = new DecimalFormat("#,##0");

    private DistanceCalculator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculates the great-circle distance between two points on Earth using the Haversine formula.
     *
     * @param lat1 Latitude of first point in decimal degrees
     * @param lon1 Longitude of first point in decimal degrees
     * @param lat2 Latitude of second point in decimal degrees
     * @param lon2 Longitude of second point in decimal degrees
     * @return Distance in miles, or null if any coordinate is null
     */
    public static Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return null;
        }

        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_MILES * c;
    }

    /**
     * Formats a distance value for display.
     *
     * @param miles Distance in miles, or null
     * @return Formatted distance string (e.g., "125 mi", "2,347 mi") or "N/A" if null
     */
    public static String formatDistance(Double miles) {
        if (miles == null) {
            return "N/A";
        }
        return DISTANCE_FORMAT.format(Math.round(miles)) + " mi";
    }

    /**
     * Formats a duration value for display as driving time.
     *
     * @param minutes Duration in minutes, or null
     * @return Formatted duration string (e.g., "45 min", "2h 15min") or empty string if null
     */
    public static String formatDuration(Double minutes) {
        if (minutes == null) {
            return "";
        }

        long totalMinutes = Math.round(minutes);
        if (totalMinutes < 60) {
            return totalMinutes + " min";
        }

        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;

        if (mins == 0) {
            return hours + "h";
        }
        return hours + "h " + mins + "min";
    }

    /**
     * Formats distance with optional duration for display.
     *
     * @param miles Distance in miles
     * @param durationMinutes Duration in minutes (can be null for fallback)
     * @param isFallback True if this is a Haversine fallback (adds "approx" indicator)
     * @return Formatted string (e.g., "125 mi (2h 15min)", "125 mi (approx)")
     */
    public static String formatDistanceWithDuration(Double miles, Double durationMinutes, boolean isFallback) {
        if (miles == null) {
            return "N/A";
        }

        String distance = DISTANCE_FORMAT.format(Math.round(miles)) + " mi";

        if (isFallback) {
            return distance + " (approx)";
        }

        if (durationMinutes != null) {
            return distance + " (" + formatDuration(durationMinutes) + ")";
        }

        return distance;
    }
}
