package dk.placestracker.util;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for DurationUtils class.
 * Validates parsing and formatting of human-friendly duration strings.
 */
class DurationUtilsTests {

    @Test
    void parseMinutesOnly() {
        Duration result = DurationUtils.parse("30min");
        assertThat(result).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void parseHoursAndMinutes() {
        Duration result = DurationUtils.parse("1h25min");
        assertThat(result).isEqualTo(Duration.ofHours(1).plusMinutes(25));
    }

    @Test
    void parseDaysHoursAndMinutes() {
        Duration result = DurationUtils.parse("2d5h30min");
        assertThat(result).isEqualTo(Duration.ofDays(2).plusHours(5).plusMinutes(30));
    }

    @Test
    void parseHoursOnly() {
        Duration result = DurationUtils.parse("3h");
        assertThat(result).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void parseDaysOnly() {
        Duration result = DurationUtils.parse("1d");
        assertThat(result).isEqualTo(Duration.ofDays(1));
    }

    @Test
    void parseDaysAndHours() {
        Duration result = DurationUtils.parse("1d12h");
        assertThat(result).isEqualTo(Duration.ofDays(1).plusHours(12));
    }

    @Test
    void parseDaysAndMinutes() {
        Duration result = DurationUtils.parse("1d30min");
        assertThat(result).isEqualTo(Duration.ofDays(1).plusMinutes(30));
    }

    @Test
    void parseCaseInsensitive() {
        Duration result1 = DurationUtils.parse("1H30MIN");
        Duration result2 = DurationUtils.parse("1h30min");
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void parseWithWhitespace() {
        Duration result = DurationUtils.parse("  1h30min  ");
        assertThat(result).isEqualTo(Duration.ofHours(1).plusMinutes(30));
    }

    @Test
    void parseRejectsEmptyString() {
        assertThatThrownBy(() -> DurationUtils.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration cannot be empty");
    }

    @Test
    void parseRejectsNull() {
        assertThatThrownBy(() -> DurationUtils.parse(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duration cannot be empty");
    }

    @Test
    void parseRejectsInvalidFormat() {
        assertThatThrownBy(() -> DurationUtils.parse("5x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid duration format");
    }

    @Test
    void parseRejectsNoComponents() {
        assertThatThrownBy(() -> DurationUtils.parse("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid duration format");
    }

    @Test
    void parseRejectsZeroDuration() {
        assertThatThrownBy(() -> DurationUtils.parse("0min"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be greater than zero");
    }

    @Test
    void parseRejectsExceeds7Days() {
        assertThatThrownBy(() -> DurationUtils.parse("8d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 7 days");
    }

    @Test
    void parseRejectsExceeds7DaysComplex() {
        assertThatThrownBy(() -> DurationUtils.parse("7d1h"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 7 days");
    }

    @Test
    void parseAccepts7DaysExactly() {
        Duration result = DurationUtils.parse("7d");
        assertThat(result).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void formatMinutesOnly() {
        Duration duration = Duration.ofMinutes(30);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("30 minutes");
    }

    @Test
    void formatHoursAndMinutes() {
        Duration duration = Duration.ofHours(1).plusMinutes(25);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 hour 25 minutes");
    }

    @Test
    void formatDaysHoursAndMinutes() {
        Duration duration = Duration.ofDays(2).plusHours(5).plusMinutes(30);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("2 days 5 hours 30 minutes");
    }

    @Test
    void formatHoursOnly() {
        Duration duration = Duration.ofHours(3);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("3 hours");
    }

    @Test
    void formatDaysOnly() {
        Duration duration = Duration.ofDays(1);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 day");
    }

    @Test
    void formatDaysAndHours() {
        Duration duration = Duration.ofDays(1).plusHours(12);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 day 12 hours");
    }

    @Test
    void formatDaysAndMinutes() {
        Duration duration = Duration.ofDays(1).plusMinutes(30);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 day 30 minutes");
    }

    @Test
    void formatZeroDuration() {
        Duration duration = Duration.ZERO;
        String result = DurationUtils.format(duration);
        assertThat(result).isEmpty();
    }

    @Test
    void formatNullDuration() {
        String result = DurationUtils.format(null);
        assertThat(result).isEmpty();
    }

    @Test
    void isValidReturnsTrueForValidDuration() {
        assertThat(DurationUtils.isValid("1h30min")).isTrue();
        assertThat(DurationUtils.isValid("30min")).isTrue();
        assertThat(DurationUtils.isValid("2d5h30min")).isTrue();
    }

    @Test
    void isValidReturnsFalseForInvalidDuration() {
        assertThat(DurationUtils.isValid("5x")).isFalse();
        assertThat(DurationUtils.isValid("abc")).isFalse();
        assertThat(DurationUtils.isValid("")).isFalse();
        assertThat(DurationUtils.isValid(null)).isFalse();
        assertThat(DurationUtils.isValid("8d")).isFalse();
        assertThat(DurationUtils.isValid("0min")).isFalse();
    }

    @Test
    void parseAndFormatRoundTrip() {
        String original = "2d5h30min";
        Duration parsed = DurationUtils.parse(original);
        String formatted = DurationUtils.format(parsed);
        assertThat(formatted).isEqualTo("2 days 5 hours 30 minutes");
    }

    @Test
    void formatSingularMinute() {
        Duration duration = Duration.ofMinutes(1);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 minute");
    }

    @Test
    void formatSingularHour() {
        Duration duration = Duration.ofHours(1);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 hour");
    }

    @Test
    void formatSingularDay() {
        Duration duration = Duration.ofDays(1);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 day");
    }

    @Test
    void formatSingularHourPluralMinutes() {
        Duration duration = Duration.ofHours(1).plusMinutes(2);
        String result = DurationUtils.format(duration);
        assertThat(result).isEqualTo("1 hour 2 minutes");
    }
}
