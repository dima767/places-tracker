package dk.placestracker.util;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and formatting visit durations.
 * Supports human-friendly format: "10min", "1h25min", "2d5h30min"
 *
 * @author Dmitriy Kopylenko
 */
public class DurationUtils {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)min)?$"
    );

    private static final Duration MAX_DURATION = Duration.ofDays(7);

    /**
     * Parse human-friendly duration format to Duration object.
     *
     * @param input Duration string (e.g., "1h25min", "2d5h30min")
     * @return Parsed Duration object
     * @throws IllegalArgumentException if format is invalid or exceeds 7 days
     */
    public static Duration parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Duration cannot be empty");
        }

        String normalized = input.trim().toLowerCase();
        Matcher matcher = DURATION_PATTERN.matcher(normalized);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid duration format. Use: 10min, 1h25min, or 2d5h30min"
            );
        }

        String daysStr = matcher.group(1);
        String hoursStr = matcher.group(2);
        String minutesStr = matcher.group(3);

        // At least one component must be present
        if (daysStr == null && hoursStr == null && minutesStr == null) {
            throw new IllegalArgumentException("Duration must specify at least days, hours, or minutes");
        }

        long days = daysStr != null ? Long.parseLong(daysStr) : 0;
        long hours = hoursStr != null ? Long.parseLong(hoursStr) : 0;
        long minutes = minutesStr != null ? Long.parseLong(minutesStr) : 0;

        Duration duration = Duration.ofDays(days)
            .plusHours(hours)
            .plusMinutes(minutes);

        if (duration.isZero()) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }

        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Duration cannot exceed 7 days");
        }

        return duration;
    }

    /**
     * Format Duration to human-friendly string.
     *
     * @param duration Duration object
     * @return Formatted string (e.g., "1 hour 25 minutes", "2 days 5 hours 30 minutes")
     */
    public static String format(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "";
        }

        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }
        if (minutes > 0) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        return sb.toString();
    }

    /**
     * Check if duration string is valid format.
     *
     * @param input Duration string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }

        try {
            parse(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
