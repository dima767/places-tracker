package dk.placestracker.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceCalculatorTests {

    @Test
    void calculateDistance_withValidCoordinates_returnsDistance() {
        // New York City to Los Angeles (approx 2,451 miles)
        Double distance = DistanceCalculator.calculateDistance(40.7128, -74.0060, 34.0522, -118.2437);

        assertThat(distance).isNotNull();
        assertThat(distance).isBetween(2440.0, 2460.0);
    }

    @Test
    void calculateDistance_withSameLocation_returnsZero() {
        Double distance = DistanceCalculator.calculateDistance(40.7128, -74.0060, 40.7128, -74.0060);

        assertThat(distance).isNotNull();
        assertThat(distance).isLessThan(0.01);  // Effectively zero
    }

    @Test
    void calculateDistance_withNullLatitude1_returnsNull() {
        Double distance = DistanceCalculator.calculateDistance(null, -74.0060, 34.0522, -118.2437);

        assertThat(distance).isNull();
    }

    @Test
    void calculateDistance_withNullLongitude2_returnsNull() {
        Double distance = DistanceCalculator.calculateDistance(40.7128, -74.0060, 34.0522, null);

        assertThat(distance).isNull();
    }

    @Test
    void formatDistance_withValidMiles_returnsFormattedString() {
        assertThat(DistanceCalculator.formatDistance(125.0)).isEqualTo("125 mi");
        assertThat(DistanceCalculator.formatDistance(2347.8)).isEqualTo("2,348 mi");
        assertThat(DistanceCalculator.formatDistance(0.0)).isEqualTo("0 mi");
        assertThat(DistanceCalculator.formatDistance(0.4)).isEqualTo("0 mi");
        assertThat(DistanceCalculator.formatDistance(0.5)).isEqualTo("1 mi");
    }

    @Test
    void formatDistance_withNull_returnsNA() {
        assertThat(DistanceCalculator.formatDistance(null)).isEqualTo("N/A");
    }

    @Test
    void formatDuration_withMinutesUnderAnHour_returnsMinutesOnly() {
        assertThat(DistanceCalculator.formatDuration(45.0)).isEqualTo("45 min");
        assertThat(DistanceCalculator.formatDuration(59.0)).isEqualTo("59 min");
        assertThat(DistanceCalculator.formatDuration(1.0)).isEqualTo("1 min");
    }

    @Test
    void formatDuration_withExactHours_returnsHoursOnly() {
        assertThat(DistanceCalculator.formatDuration(60.0)).isEqualTo("1h");
        assertThat(DistanceCalculator.formatDuration(120.0)).isEqualTo("2h");
        assertThat(DistanceCalculator.formatDuration(180.0)).isEqualTo("3h");
    }

    @Test
    void formatDuration_withHoursAndMinutes_returnsFormattedString() {
        assertThat(DistanceCalculator.formatDuration(90.0)).isEqualTo("1h 30min");
        assertThat(DistanceCalculator.formatDuration(135.0)).isEqualTo("2h 15min");
        assertThat(DistanceCalculator.formatDuration(61.0)).isEqualTo("1h 1min");
    }

    @Test
    void formatDuration_withNull_returnsEmptyString() {
        assertThat(DistanceCalculator.formatDuration(null)).isEmpty();
    }

    @Test
    void formatDistanceWithDuration_withApiResult_returnsFullFormat() {
        String result = DistanceCalculator.formatDistanceWithDuration(125.0, 135.0, false);
        assertThat(result).isEqualTo("125 mi (2h 15min)");
    }

    @Test
    void formatDistanceWithDuration_withFallback_returnsApproxIndicator() {
        String result = DistanceCalculator.formatDistanceWithDuration(125.0, null, true);
        assertThat(result).isEqualTo("125 mi (approx)");
    }

    @Test
    void formatDistanceWithDuration_withNullMiles_returnsNA() {
        String result = DistanceCalculator.formatDistanceWithDuration(null, 135.0, false);
        assertThat(result).isEqualTo("N/A");
    }

    @Test
    void formatDistanceWithDuration_withMilesButNoDuration_returnsMilesOnly() {
        String result = DistanceCalculator.formatDistanceWithDuration(125.0, null, false);
        assertThat(result).isEqualTo("125 mi");
    }
}
