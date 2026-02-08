package dk.placestracker.service;

import dk.placestracker.domain.model.Place;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing Place entities.
 *
 * @author Dmitriy Kopylenko
 */
public interface PlaceService {

    /**
     * Find all places
     */
    List<Place> findAll();

    /**
     * Find all places ordered by most recent visit date (newest first)
     */
    List<Place> findAllByMostRecentVisit();

    /**
     * Find all places ordered by name
     */
    List<Place> findAllByName();

    /**
     * Find a place by ID
     */
    Optional<Place> findById(String id);

    /**
     * Create a new place (sets createdAt and updatedAt timestamps)
     */
    Place create(Place place);

    /**
     * Update an existing place (preserves createdAt, updates updatedAt timestamp)
     */
    Place update(String id, Place place);

    /**
     * Delete a place by ID
     */
    void deleteById(String id);

    /**
     * Search places by name, location, or state
     */
    List<Place> search(String searchTerm);

    /**
     * Find places by state
     */
    List<Place> findByState(String state);

    /**
     * Find places by country
     */
    List<Place> findByCountry(String country);

    /**
     * Count places by state
     */
    long countByState(String state);

    /**
     * Count places by country
     */
    long countByCountry(String country);

    /**
     * Get total place count
     */
    long count();

    // Favorites & Wishlist

    /**
     * Find all visited places (excludes wishlist items)
     */
    List<Place> findAllVisited();

    /**
     * Find all visited places ordered by most recent visit date
     */
    List<Place> findAllVisitedByMostRecentVisit();

    /**
     * Find all wishlist (TO_VISIT) places
     */
    List<Place> findAllWishlist();

    /**
     * Find all favorite places
     */
    List<Place> findFavorites();

    /**
     * Toggle the favorite flag on a place
     */
    Place toggleFavorite(String id);

    /**
     * Convert a wishlist item to visited status
     */
    Place convertToVisited(String id);

    /**
     * Search visited places only
     */
    List<Place> searchVisited(String searchTerm);

    /**
     * Search wishlist places only
     */
    List<Place> searchWishlist(String searchTerm);

    /**
     * Count wishlist places
     */
    long countWishlist();

    /**
     * Count favorite places
     */
    long countFavorites();
}
