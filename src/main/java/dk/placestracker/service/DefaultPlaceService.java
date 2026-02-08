package dk.placestracker.service;

import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of PlaceService.
 *
 * @author Dmitriy Kopylenko
 */
@Service
@Transactional
public class DefaultPlaceService implements PlaceService {

    private final PlaceRepository placeRepository;
    private final PhotoService photoService;

    public DefaultPlaceService(PlaceRepository placeRepository, PhotoService photoService) {
        this.placeRepository = placeRepository;
        this.photoService = photoService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findAll() {
        return placeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findAllByMostRecentVisit() {
        List<Place> places = placeRepository.findAllVisitedPlaces();
        // Sort by most recent visit date (descending)
        places.sort((p1, p2) -> {
            var v1 = p1.getMostRecentVisit();
            var v2 = p2.getMostRecentVisit();

            // Places with no visits go last
            if (v1.isEmpty() && v2.isEmpty()) return 0;
            if (v1.isEmpty()) return 1;
            if (v2.isEmpty()) return -1;

            // Compare dates descending (most recent first)
            return v2.get().date().compareTo(v1.get().date());
        });
        return places;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findAllByName() {
        return placeRepository.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Place> findById(String id) {
        return placeRepository.findById(id);
    }

    @Override
    public Place create(Place place) {
        LocalDateTime now = LocalDateTime.now();
        // Set both createdAt and updatedAt for new places
        Place placeWithTimestamps = place.withTimestamps(now, now);
        return placeRepository.save(placeWithTimestamps);
    }

    @Override
    public Place update(String id, Place place) {
        return placeRepository.findById(id)
                .map(existingPlace -> {
                    // IMPORTANT: Ensure all visits have IDs (generate for new visits)
                    List<dk.placestracker.domain.model.Visit> visitsWithIds = place.visits().stream()
                            .map(visit -> {
                                if (visit.id() == null || visit.id().isBlank()) {
                                    // New visit added on edit page - generate ID
                                    return dk.placestracker.domain.model.Visit.create(
                                            visit.date(),
                                            visit.temperatureF(),
                                            visit.notes(),
                                            visit.duration()
                                    ).withPhotos(visit.photoIds() != null ? visit.photoIds() : new java.util.ArrayList<>());
                                }
                                return visit;
                            })
                            .toList();

                    // Detect removed visits and cascade delete their photos
                    List<String> oldVisitIds = existingPlace.visits().stream()
                            .map(dk.placestracker.domain.model.Visit::id)
                            .toList();
                    List<String> newVisitIds = visitsWithIds.stream()
                            .map(dk.placestracker.domain.model.Visit::id)
                            .toList();

                    // Find visits that were removed
                    oldVisitIds.stream()
                            .filter(visitId -> !newVisitIds.contains(visitId))
                            .forEach(removedVisitId -> {
                                // Cascade delete photos for removed visit
                                photoService.deletePhotosByVisitId(removedVisitId);
                            });

                    // Preserve createdAt from existing place, update updatedAt to now
                    Place updatedPlace = existingPlace.withUpdate(
                            place.name(),
                            place.location(),
                            place.state(),
                            place.country(),
                            visitsWithIds,  // Use visits with guaranteed IDs
                            place.hasToilet(),
                            place.latitude(),
                            place.longitude(),
                            place.formattedAddress(),
                            place.googlePlaceId(),
                            place.website(),
                            place.phoneNumber(),
                            place.googleRating(),
                            place.googleReviewCount(),
                            LocalDateTime.now()
                    );
                    return placeRepository.save(updatedPlace);
                })
                .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));
    }

    @Override
    public void deleteById(String id) {
        Place place = placeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));

        // Cascade delete all photos for all visits
        if (place.visits() != null) {
            place.visits().forEach(visit -> {
                photoService.deletePhotosByVisitId(visit.id());
            });
        }

        placeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> search(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return findAll();
        }
        return placeRepository.searchPlaces(searchTerm.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findByState(String state) {
        return placeRepository.findByStateIgnoreCase(state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findByCountry(String country) {
        return placeRepository.findByCountryIgnoreCase(country);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByState(String state) {
        return placeRepository.countByStateIgnoreCase(state);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCountry(String country) {
        return placeRepository.countByCountryIgnoreCase(country);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return placeRepository.count();
    }

    // Favorites & Wishlist

    @Override
    @Transactional(readOnly = true)
    public List<Place> findAllVisited() {
        return placeRepository.findAllVisitedPlaces();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findAllVisitedByMostRecentVisit() {
        List<Place> places = placeRepository.findAllVisitedPlaces();
        places.sort((p1, p2) -> {
            var v1 = p1.getMostRecentVisit();
            var v2 = p2.getMostRecentVisit();
            if (v1.isEmpty() && v2.isEmpty()) return 0;
            if (v1.isEmpty()) return 1;
            if (v2.isEmpty()) return -1;
            return v2.get().date().compareTo(v1.get().date());
        });
        return places;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findAllWishlist() {
        return placeRepository.findByStatus("TO_VISIT");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> findFavorites() {
        return placeRepository.findFavoriteVisitedPlaces();
    }

    @Override
    public Place toggleFavorite(String id) {
        return placeRepository.findById(id)
                .map(place -> {
                    Place toggled = place.withFavorite(!place.favorite());
                    return placeRepository.save(toggled);
                })
                .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));
    }

    @Override
    public Place convertToVisited(String id) {
        return placeRepository.findById(id)
                .map(place -> {
                    Place visited = place.withStatus("VISITED");
                    return placeRepository.save(visited);
                })
                .orElseThrow(() -> new IllegalArgumentException("Place not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> searchVisited(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return findAllVisited();
        }
        return placeRepository.searchVisitedPlaces(searchTerm.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Place> searchWishlist(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return findAllWishlist();
        }
        return placeRepository.searchWishlistPlaces(searchTerm.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public long countWishlist() {
        return placeRepository.countWishlistPlaces();
    }

    @Override
    @Transactional(readOnly = true)
    public long countFavorites() {
        return placeRepository.countByFavoriteTrue();
    }
}
