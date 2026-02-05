package dk.placestracker.web.dto;

import dk.placestracker.domain.model.Place;

import java.util.List;

/**
 * DTO for paginated place results with sorting information.
 *
 * @author Dmitriy Kopylenko
 */
public record PlacePage(
        List<Place> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortField,
        String sortDirection,
        boolean hasNext,
        boolean hasPrevious
) {
    /**
     * Create a PlacePage from a list of places with pagination applied.
     */
    public static PlacePage of(List<Place> allPlaces, int page, int size, String sortField, String sortDirection) {
        int totalElements = allPlaces.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Ensure page is within bounds
        int safePage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));

        // Apply pagination
        int fromIndex = safePage * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<Place> content = fromIndex < totalElements
                ? allPlaces.subList(fromIndex, toIndex)
                : List.of();

        return new PlacePage(
                content,
                safePage,
                size,
                totalElements,
                totalPages,
                sortField,
                sortDirection,
                safePage < totalPages - 1,
                safePage > 0
        );
    }

    /**
     * Get the opposite sort direction for toggling.
     */
    public String getOppositeDirection() {
        return "asc".equalsIgnoreCase(sortDirection) ? "desc" : "asc";
    }

    /**
     * Check if currently sorted by a specific field.
     */
    public boolean isSortedBy(String field) {
        return field != null && field.equalsIgnoreCase(sortField);
    }
}
