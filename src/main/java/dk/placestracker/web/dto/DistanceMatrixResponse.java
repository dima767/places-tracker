package dk.placestracker.web.dto;

import java.util.List;

/**
 * DTO for parsing Google Distance Matrix API JSON response.
 *
 * @author Dmitriy Kopylenko
 *
 * Example response:
 * {
 *   "status": "OK",
 *   "rows": [{
 *     "elements": [{
 *       "status": "OK",
 *       "distance": { "value": 200375, "text": "125 mi" },
 *       "duration": { "value": 8100, "text": "2 hours 15 mins" }
 *     }]
 *   }]
 * }
 */
public record DistanceMatrixResponse(
        String status,
        List<String> destination_addresses,
        List<String> origin_addresses,
        List<Row> rows
) {
    public record Row(List<Element> elements) {}

    public record Element(
            String status,
            Distance distance,
            Duration duration
    ) {
        /**
         * Check if this element has valid distance data.
         */
        public boolean isOk() {
            return "OK".equals(status);
        }
    }

    public record Distance(
            /** Distance in meters */
            int value,
            /** Human-readable distance (e.g., "125 mi") */
            String text
    ) {
        /**
         * Convert meters to miles.
         */
        public double toMiles() {
            return value / 1609.344;
        }
    }

    public record Duration(
            /** Duration in seconds */
            int value,
            /** Human-readable duration (e.g., "2 hours 15 mins") */
            String text
    ) {
        /**
         * Convert seconds to minutes.
         */
        public double toMinutes() {
            return value / 60.0;
        }
    }

    /**
     * Check if the overall response is successful.
     */
    public boolean isOk() {
        return "OK".equals(status);
    }

    /**
     * Get the element at the specified index (for single origin, multiple destinations).
     * Returns null if index is out of bounds or no rows exist.
     */
    public Element getElement(int destinationIndex) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        Row row = rows.get(0);
        if (row.elements() == null || destinationIndex >= row.elements().size()) {
            return null;
        }
        return row.elements().get(destinationIndex);
    }
}
