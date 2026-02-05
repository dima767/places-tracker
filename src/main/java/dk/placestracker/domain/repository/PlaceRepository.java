package dk.placestracker.domain.repository;

import dk.placestracker.domain.model.Place;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for Place entities.
 *
 * @author Dmitriy Kopylenko
 */
@Repository
public interface PlaceRepository extends MongoRepository<Place, String> {

    // Find places by state
    List<Place> findByStateIgnoreCase(String state);

    // Find places by country
    List<Place> findByCountryIgnoreCase(String country);

    // Find places by name (partial match, case-insensitive)
    List<Place> findByNameContainingIgnoreCase(String name);

    // Find places by state and country
    List<Place> findByStateIgnoreCaseAndCountryIgnoreCase(String state, String country);

    // Search places by name, location, or state (for htmx search)
    @Query("{ $or: [ " +
           "{ 'name': { $regex: ?0, $options: 'i' } }, " +
           "{ 'location': { $regex: ?0, $options: 'i' } }, " +
           "{ 'state': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Place> searchPlaces(String searchTerm);

    // Find all places ordered by name
    List<Place> findAllByOrderByNameAsc();

    // Count places by state
    long countByStateIgnoreCase(String state);

    // Count places by country
    long countByCountryIgnoreCase(String country);
}
