package dk.placestracker.web.controller;

import dk.placestracker.domain.model.Country;
import dk.placestracker.domain.model.Place;
import dk.placestracker.domain.model.StateProvince;
import dk.placestracker.service.DistanceResult;
import dk.placestracker.service.DistanceService;
import dk.placestracker.service.GoogleMapsService;
import dk.placestracker.service.PlaceService;
import dk.placestracker.service.PhotoService;
import dk.placestracker.service.ReferenceDataService;
import dk.placestracker.service.SettingsService;
import dk.placestracker.util.DistanceCalculator;
import dk.placestracker.util.DurationUtils;
import dk.placestracker.web.dto.PlaceExtractResponse;
import dk.placestracker.web.dto.PlacePage;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

/**
 * Controller for Place CRUD operations and related functionality.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
@RequestMapping("/places")
public class PlaceController {

    private static final Logger logger = LoggerFactory.getLogger(PlaceController.class);

    private final PlaceService placeService;
    private final ReferenceDataService referenceDataService;
    private final GoogleMapsService googleMapsService;
    private final PhotoService photoService;
    private final SettingsService settingsService;
    private final DistanceService distanceService;
    private final ObjectMapper objectMapper;
    private final int maxPhotoSizeMB;

    public PlaceController(PlaceService placeService,
                         ReferenceDataService referenceDataService,
                         GoogleMapsService googleMapsService,
                         PhotoService photoService,
                         SettingsService settingsService,
                         DistanceService distanceService,
                         ObjectMapper objectMapper,
                         @Value("${app.photo.max-size-mb}") int maxPhotoSizeMB) {
        this.placeService = placeService;
        this.referenceDataService = referenceDataService;
        this.googleMapsService = googleMapsService;
        this.photoService = photoService;
        this.settingsService = settingsService;
        this.distanceService = distanceService;
        this.objectMapper = objectMapper;
        this.maxPhotoSizeMB = maxPhotoSizeMB;
    }

    @ModelAttribute("countries")
    public List<Country> countries() {
        return referenceDataService.getAllCountries();
    }

    @ModelAttribute("statesProvinces")
    public List<StateProvince> statesProvinces() {
        return referenceDataService.getAllStatesProvinces();
    }

    @ModelAttribute("statesProvincesByCountry")
    public Map<String, List<StateProvince>> statesProvincesByCountry() {
        return referenceDataService.getAllStatesProvinces().stream()
                .collect(Collectors.groupingBy(StateProvince::countryCode));
    }

    @ModelAttribute("maxPhotoSizeMB")
    public int maxPhotoSizeMB() {
        return maxPhotoSizeMB;
    }

    private static final int DEFAULT_PAGE_SIZE = 10;

    @GetMapping
    public String listPlaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastVisit") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        // Validate and cap page size
        int pageSize = Math.min(Math.max(size, 5), 50);

        // Get visited places only (exclude wishlist) and calculate distances
        List<Place> allPlaces = placeService.findAllVisited();
        Map<String, DistanceResult> distances = distanceService.getDistances(allPlaces);

        // Sort places based on sort field
        List<Place> sortedPlaces = sortPlaces(allPlaces, distances, sort, direction);

        // Create paginated result
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());
        model.addAttribute("placePage", placePage);
        model.addAttribute("distances", distances);
        return "places/list";
    }

    /**
     * Sort places by the specified field and direction.
     */
    private List<Place> sortPlaces(List<Place> places, Map<String, DistanceResult> distances,
                                   String sortField, String sortDirection) {
        Comparator<Place> comparator = switch (sortField.toLowerCase()) {
            case "name" -> Comparator.comparing(
                    (Place p) -> p.name(),
                    String.CASE_INSENSITIVE_ORDER
            );
            case "distance" -> Comparator.comparing(
                    (Place p) -> {
                        DistanceResult result = distances.get(p.id());
                        // Use cached driving distance if available, otherwise fallback miles
                        if (result != null && result.hasDistance()) {
                            return result.miles();
                        }
                        return Double.MAX_VALUE; // Places without distance go to end
                    },
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case "lastvisit", "lastVisit" -> Comparator.comparing(
                    (Place p) -> p.getMostRecentVisit()
                            .map(v -> v.date())
                            .orElse(null),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            default -> Comparator.comparing(
                    (Place p) -> p.getMostRecentVisit()
                            .map(v -> v.date())
                            .orElse(null),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        };

        // Apply direction
        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return places.stream()
                .sorted(comparator)
                .toList();
    }

    @GetMapping("/{id}")
    public String viewPlace(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return placeService.findById(id)
                .map(place -> {
                    // Clean up any missing photos before displaying
                    Place cleanedPlace = cleanupMissingPhotos(place);
                    DistanceResult distanceFromHome = distanceService.getDistance(cleanedPlace);
                    model.addAttribute("place", cleanedPlace);
                    model.addAttribute("distanceFromHome", distanceFromHome);
                    return "places/detail";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Place not found");
                    return "redirect:/places";
                });
    }

    @GetMapping("/new")
    public String createPlaceForm(Model model) {
        model.addAttribute("place", Place.create("", "", "", "",
                new ArrayList<>(), false, null, null, null, null, null, null, null, null, new ArrayList<>()));
        return "places/create";
    }

    @PostMapping
    public String createPlace(@Valid @ModelAttribute("place") Place place,
                            BindingResult bindingResult,
                            @RequestParam(required = false) String googleReviewsJson,
                            jakarta.servlet.http.HttpServletRequest request,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            return "places/create";
        }

        try {
            // Extract indexed duration parameters (Spring doesn't auto-bind visitDurations[0], visitDurations[1], etc.)
            int visitCount = place.visits() != null ? place.visits().size() : 0;
            String[] visitDurations = extractIndexedParameters(request, "visitDurations", visitCount);

            logger.debug("createPlace - visits: {}, visitDurations: {}",
                visitCount,
                java.util.Arrays.toString(visitDurations));

            // Parse Google reviews from JSON if present
            List<dk.placestracker.domain.model.Review> googleReviews = parseGoogleReviews(googleReviewsJson);

            // Parse visit durations from form input
            List<dk.placestracker.domain.model.Visit> visitsWithDurations = parseVisitDurations(place.visits(), visitDurations, model);

            // Update place with parsed reviews and durations
            place = new Place(place.id(), place.name(), place.location(), place.state(), place.country(),
                    visitsWithDurations, place.hasToilet(), place.latitude(), place.longitude(),
                    place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                    place.googleRating(), place.googleReviewCount(),
                    googleReviews != null && !googleReviews.isEmpty() ? googleReviews : place.googleReviews(),
                    place.createdAt(), place.updatedAt(),
                    null, null, null, null,
                    place.favorite(), place.status());

            // Step 1: Create the place first (generates IDs for place and visits)
            Place savedPlace = placeService.create(place);

            // Step 2: Extract and upload photos for each visit if provided
            List<dk.placestracker.domain.model.Visit> updatedVisits = new ArrayList<>(savedPlace.visits());
            boolean photosUploaded = false;

            // Process photos for each visit index
            for (int i = 0; i < updatedVisits.size(); i++) {
                // Get photos for this visit index
                String paramName = "visitPhotos[" + i + "]";
                List<MultipartFile> visitPhotosAtIndex = new ArrayList<>();

                if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest) {
                    List<MultipartFile> files = multipartRequest.getFiles(paramName);
                    if (files != null) {
                        visitPhotosAtIndex.addAll(files);
                    }
                }

                // Skip if no files selected for this visit
                if (visitPhotosAtIndex.isEmpty() ||
                    (visitPhotosAtIndex.size() == 1 && visitPhotosAtIndex.get(0).isEmpty())) {
                    continue;
                }

                dk.placestracker.domain.model.Visit visit = updatedVisits.get(i);

                // Upload photos for this visit
                try {
                    List<String> photoIds = photoService.savePhotosForVisit(
                            visitPhotosAtIndex,
                            savedPlace.id(),
                            visit.id()
                    );

                    // Update visit with photo IDs
                    updatedVisits.set(i, visit.withPhotos(photoIds));
                    photosUploaded = true;
                } catch (Exception photoEx) {
                    // Log error but continue with other photos
                    redirectAttributes.addFlashAttribute("warning",
                        "Some photos could not be uploaded for visit " + (i + 1) + ": " + photoEx.getMessage());
                }
            }

            // Step 3: Update the place with photo IDs if any were uploaded
            if (photosUploaded) {
                Place placeWithPhotos = savedPlace.withUpdate(
                        savedPlace.name(), savedPlace.location(), savedPlace.state(), savedPlace.country(),
                        updatedVisits, savedPlace.hasToilet(),
                        savedPlace.latitude(), savedPlace.longitude(),
                        savedPlace.formattedAddress(), savedPlace.googlePlaceId(), savedPlace.website(), savedPlace.phoneNumber(),
                        savedPlace.googleRating(), savedPlace.googleReviewCount(),
                        java.time.LocalDateTime.now()
                );
                placeService.update(savedPlace.id(), placeWithPhotos);
            }

            redirectAttributes.addFlashAttribute("success", "Place created successfully!");
            return "redirect:/places/" + savedPlace.id();
        } catch (Exception e) {
            model.addAttribute("error", "Error creating place: " + e.getMessage());
            return "places/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editPlaceForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        return placeService.findById(id)
                .map(place -> {
                    // Clean up any missing photos before editing
                    Place cleanedPlace = cleanupMissingPhotos(place);
                    model.addAttribute("place", cleanedPlace);
                    return "places/edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Place not found");
                    return "redirect:/places";
                });
    }

    @PostMapping("/{id}")
    public String updatePlace(@PathVariable String id,
                            @Valid @ModelAttribute("place") Place place,
                            BindingResult bindingResult,
                            @RequestParam(required = false) String googleReviewsJson,
                            jakarta.servlet.http.HttpServletRequest request,
                            RedirectAttributes redirectAttributes,
                            Model model) {
        if (bindingResult.hasErrors()) {
            return "places/edit";
        }

        try {
            // Extract indexed duration parameters (Spring doesn't auto-bind visitDurations[0], visitDurations[1], etc.)
            int visitCount = place.visits() != null ? place.visits().size() : 0;
            String[] visitDurations = extractIndexedParameters(request, "visitDurations", visitCount);

            logger.debug("updatePlace - placeId: {}, visits: {}, visitDurations: {}",
                id,
                visitCount,
                java.util.Arrays.toString(visitDurations));

            // Parse Google reviews from JSON if present
            List<dk.placestracker.domain.model.Review> googleReviews = parseGoogleReviews(googleReviewsJson);

            // Parse visit durations from form input
            List<dk.placestracker.domain.model.Visit> visitsWithDurations = parseVisitDurations(place.visits(), visitDurations, model);

            // Update place with parsed reviews and durations
            place = new Place(place.id(), place.name(), place.location(), place.state(), place.country(),
                    visitsWithDurations, place.hasToilet(), place.latitude(), place.longitude(),
                    place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                    place.googleRating(), place.googleReviewCount(),
                    googleReviews != null && !googleReviews.isEmpty() ? googleReviews : place.googleReviews(),
                    place.createdAt(), place.updatedAt(),
                    place.drivingDistanceMiles(), place.drivingDurationMinutes(),
                    place.distanceCalculatedAt(), place.distanceFromHomeLatLng(),
                    place.favorite(), place.status());

            // Step 1: Update the place (generates IDs for new visits via service layer)
            Place updatedPlace = placeService.update(id, place);

            // Step 2: Check if any photos were selected for new visits (via file inputs)
            List<dk.placestracker.domain.model.Visit> visitsWithPhotos = new ArrayList<>(updatedPlace.visits());
            boolean photosUploaded = false;

            // Process photos for each visit index
            for (int i = 0; i < visitsWithPhotos.size(); i++) {
                String paramName = "visitPhotos[" + i + "]";
                List<MultipartFile> visitPhotosAtIndex = new ArrayList<>();

                if (request instanceof org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest) {
                    List<MultipartFile> files = multipartRequest.getFiles(paramName);
                    if (files != null) {
                        visitPhotosAtIndex.addAll(files);
                    }
                }

                // Skip if no files selected for this visit
                if (visitPhotosAtIndex.isEmpty() ||
                    (visitPhotosAtIndex.size() == 1 && visitPhotosAtIndex.get(0).isEmpty())) {
                    continue;
                }

                dk.placestracker.domain.model.Visit visit = visitsWithPhotos.get(i);

                // Upload photos for this visit
                try {
                    List<String> existingPhotoIds = visit.photoIds() != null ? visit.photoIds() : new ArrayList<>();
                    List<String> newPhotoIds = photoService.savePhotosForVisit(
                            visitPhotosAtIndex,
                            updatedPlace.id(),
                            visit.id()
                    );

                    // Combine existing and new photo IDs
                    List<String> allPhotoIds = new ArrayList<>(existingPhotoIds);
                    allPhotoIds.addAll(newPhotoIds);

                    visitsWithPhotos.set(i, visit.withPhotos(allPhotoIds));
                    photosUploaded = true;
                } catch (Exception photoEx) {
                    redirectAttributes.addFlashAttribute("warning",
                        "Some photos could not be uploaded for visit " + (i + 1) + ": " + photoEx.getMessage());
                }
            }

            // Step 3: Update place again if photos were uploaded
            if (photosUploaded) {
                Place placeWithPhotos = updatedPlace.withUpdate(
                        updatedPlace.name(), updatedPlace.location(), updatedPlace.state(), updatedPlace.country(),
                        visitsWithPhotos, updatedPlace.hasToilet(),
                        updatedPlace.latitude(), updatedPlace.longitude(),
                        updatedPlace.formattedAddress(), updatedPlace.googlePlaceId(), updatedPlace.website(), updatedPlace.phoneNumber(),
                        updatedPlace.googleRating(), updatedPlace.googleReviewCount(),
                        java.time.LocalDateTime.now()
                );
                placeService.update(id, placeWithPhotos);
            }

            redirectAttributes.addFlashAttribute("success", "Place updated successfully!");
            return "redirect:/places/" + id;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Place not found");
            return "redirect:/places";
        } catch (Exception e) {
            model.addAttribute("error", "Error updating place: " + e.getMessage());
            return "places/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deletePlace(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            // Delete the place (photos are cascade deleted by service layer)
            placeService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Place deleted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Place not found");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting place: " + e.getMessage());
        }
        return "redirect:/places";
    }

    @GetMapping("/search")
    public String searchPlaces(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastVisit") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model) {

        // Validate and cap page size
        int pageSize = Math.min(Math.max(size, 5), 50);

        List<Place> places;
        if (q == null || q.isBlank()) {
            places = placeService.findAllVisited();
        } else {
            places = placeService.searchVisited(q);
        }

        Map<String, DistanceResult> distances = distanceService.getDistances(places);

        // Sort places
        List<Place> sortedPlaces = sortPlaces(places, distances, sort, direction);

        // Create paginated result
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());
        model.addAttribute("placePage", placePage);
        model.addAttribute("distances", distances);
        model.addAttribute("searchQuery", q);
        // Return a fragment for htmx
        return "places/list :: places-table";
    }

    // ===== Favorites =====

    @PostMapping("/{id}/toggle-favorite")
    public String toggleFavorite(@PathVariable String id,
                                 @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                 Model model) {
        Place place = placeService.toggleFavorite(id);
        model.addAttribute("place", place);
        if ("true".equals(hxRequest)) {
            return "places/fragments/favorite-button :: favorite-btn";
        }
        return "redirect:/places/" + id;
    }

    @GetMapping("/favorites")
    public String listFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        int pageSize = Math.min(Math.max(size, 5), 50);
        List<Place> allPlaces = placeService.findFavorites();
        Map<String, DistanceResult> distances = distanceService.getDistances(allPlaces);
        List<Place> sortedPlaces = sortPlaces(allPlaces, distances, sort, direction);
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());
        model.addAttribute("placePage", placePage);
        model.addAttribute("distances", distances);
        model.addAttribute("listType", "favorites");
        return "places/list";
    }

    @GetMapping("/favorites/search")
    public String searchFavorites(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        int pageSize = Math.min(Math.max(size, 5), 50);
        List<Place> places;
        if (q == null || q.isBlank()) {
            places = placeService.findFavorites();
        } else {
            // Search favorites by filtering favorite visited places matching search
            places = placeService.searchVisited(q).stream()
                    .filter(Place::favorite)
                    .toList();
        }

        Map<String, DistanceResult> distances = distanceService.getDistances(places);
        List<Place> sortedPlaces = sortPlaces(places, distances, sort, direction);
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());
        model.addAttribute("placePage", placePage);
        model.addAttribute("distances", distances);
        model.addAttribute("searchQuery", q);
        model.addAttribute("listType", "favorites");
        return "places/list :: places-table";
    }

    // ===== Wishlist =====

    @GetMapping("/wishlist")
    public String listWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        int pageSize = Math.min(Math.max(size, 5), 50);
        List<Place> allPlaces = placeService.findAllWishlist();
        Map<String, DistanceResult> distances = distanceService.getDistances(allPlaces);
        List<Place> sortedPlaces = sortPlaces(allPlaces, distances, sort, direction);
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());
        model.addAttribute("placePage", placePage);
        model.addAttribute("distances", distances);
        model.addAttribute("listType", "wishlist");
        return "places/wishlist-list";
    }

    @GetMapping("/wishlist/new")
    public String createWishlistItemForm(Model model) {
        model.addAttribute("place", Place.createWishlistItem("", "", "", "",
                null, null, null, null, null, null, null, null, new ArrayList<>()));
        return "places/wishlist-create";
    }

    @PostMapping("/wishlist")
    public String createWishlistItem(@Valid @ModelAttribute("place") Place place,
                                     BindingResult bindingResult,
                                     @RequestParam(required = false) String googleReviewsJson,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            return "places/wishlist-create";
        }

        try {
            List<dk.placestracker.domain.model.Review> googleReviews = parseGoogleReviews(googleReviewsJson);

            Place wishlistItem = Place.createWishlistItem(
                    place.name(), place.location(), place.state(), place.country(),
                    place.latitude(), place.longitude(),
                    place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                    place.googleRating(), place.googleReviewCount(),
                    googleReviews != null && !googleReviews.isEmpty() ? googleReviews : place.googleReviews()
            );

            Place saved = placeService.create(wishlistItem);
            redirectAttributes.addFlashAttribute("success", "Added to wishlist!");
            return "redirect:/places/" + saved.id();
        } catch (Exception e) {
            model.addAttribute("error", "Error adding to wishlist: " + e.getMessage());
            return "places/wishlist-create";
        }
    }

    @GetMapping("/wishlist/search")
    public String searchWishlist(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {

        int pageSize = Math.min(Math.max(size, 5), 50);
        List<Place> places;
        if (q == null || q.isBlank()) {
            places = placeService.findAllWishlist();
        } else {
            places = placeService.searchWishlist(q);
        }

        Map<String, DistanceResult> distances = distanceService.getDistances(places);
        List<Place> sortedPlaces = sortPlaces(places, distances, sort, direction);
        PlacePage placePage = PlacePage.of(sortedPlaces, page, pageSize, sort, direction);

        model.addAttribute("places", placePage.content());
        model.addAttribute("placePage", placePage);
        model.addAttribute("distances", distances);
        model.addAttribute("searchQuery", q);
        model.addAttribute("listType", "wishlist");
        return "places/wishlist-list :: places-table";
    }

    @PostMapping("/{id}/convert-to-visited")
    public String convertToVisited(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            placeService.convertToVisited(id);
            redirectAttributes.addFlashAttribute("success", "Moved to visited places! You can now add visits and photos.");
            return "redirect:/places/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Place not found");
            return "redirect:/places/wishlist";
        }
    }

    @GetMapping("/states")
    public String getStatesByCountry(@RequestParam(required = false) String country,
                                     @RequestParam(required = false) String selectedState,
                                     Model model) {
        if (country != null && !country.isBlank()) {
            List<StateProvince> states = referenceDataService.getAllStatesProvinces().stream()
                    .filter(sp -> sp.countryCode().equals(country))
                    .toList();
            model.addAttribute("states", states);
        } else {
            model.addAttribute("states", List.of());
        }
        model.addAttribute("selectedState", selectedState);
        // Return a fragment for htmx
        return "places/form :: state-options";
    }

    /**
     * Extract place information from a Google Maps URL.
     * Returns JSON response with extracted place data or error message.
     */
    @PostMapping("/extract-from-url")
    @ResponseBody
    public PlaceExtractResponse extractPlaceFromUrl(@RequestParam String mapsUrl) {
        return googleMapsService.extractPlaceFromUrl(mapsUrl);
    }

    /**
     * Upload a single photo for a visit.
     * Primarily for AJAX requests from the edit page.
     */
    @PostMapping("/{placeId}/visits/{visitId}/photos")
    @ResponseBody
    public ResponseEntity<String> uploadPhotoForVisit(@PathVariable String placeId,
                                                       @PathVariable String visitId,
                                                       @RequestParam("photo") MultipartFile photo) {
        try {
            // Store photo in GridFS with visit metadata
            String photoId = photoService.savePhotoForVisit(photo, placeId, visitId);

            // Find place and visit, add photo ID
            Place place = placeService.findById(placeId)
                    .orElseThrow(() -> new IllegalArgumentException("Place not found"));

            // Update the visit with the new photo
            List<dk.placestracker.domain.model.Visit> updatedVisits = place.visits().stream()
                    .map(v -> v.id().equals(visitId) ? v.withAddedPhoto(photoId) : v)
                    .toList();

            // Update place with modified visits
            Place updatedPlace = place.withUpdate(
                    place.name(), place.location(), place.state(), place.country(),
                    updatedVisits, place.hasToilet(),
                    place.latitude(), place.longitude(),
                    place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                    place.googleRating(), place.googleReviewCount(),
                    java.time.LocalDateTime.now()
            );
            placeService.update(placeId, updatedPlace);

            return ResponseEntity.ok(photoId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error uploading photo: " + e.getMessage());
        }
    }

    /**
     * Upload multiple photos for a visit (bulk upload).
     * Primarily for AJAX requests from the edit page.
     */
    @PostMapping("/{placeId}/visits/{visitId}/photos/bulk")
    @ResponseBody
    public ResponseEntity<String> uploadPhotosForVisit(@PathVariable String placeId,
                                                        @PathVariable String visitId,
                                                        @RequestParam("photos") List<MultipartFile> photos) {
        try {
            // Store all photos in GridFS with visit metadata
            List<String> photoIds = photoService.savePhotosForVisit(photos, placeId, visitId);

            // Find place and visit, add all photo IDs
            Place place = placeService.findById(placeId)
                    .orElseThrow(() -> new IllegalArgumentException("Place not found"));

            // Update the visit with the new photos
            List<dk.placestracker.domain.model.Visit> updatedVisits = place.visits().stream()
                    .map(v -> {
                        if (v.id().equals(visitId)) {
                            List<String> newPhotoIds = new ArrayList<>(v.photoIds() != null ? v.photoIds() : new ArrayList<>());
                            newPhotoIds.addAll(photoIds);
                            return v.withPhotos(newPhotoIds);
                        }
                        return v;
                    })
                    .toList();

            // Update place with modified visits
            Place updatedPlace = place.withUpdate(
                    place.name(), place.location(), place.state(), place.country(),
                    updatedVisits, place.hasToilet(),
                    place.latitude(), place.longitude(),
                    place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                    place.googleRating(), place.googleReviewCount(),
                    java.time.LocalDateTime.now()
            );
            placeService.update(placeId, updatedPlace);

            return ResponseEntity.ok("Uploaded " + photoIds.size() + " photos successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error uploading photos: " + e.getMessage());
        }
    }

    /**
     * Download/view a photo with HTTP caching and streaming support.
     * Photos are immutable (same ID = same content), so we can cache aggressively.
     * Uses InputStreamResource to stream without loading full photo into memory.
     */
    @GetMapping("/photos/{photoId}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String photoId,
                                             @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        // ETag based on photo ID (content never changes for same ID)
        String etag = "\"" + photoId + "\"";

        // Return 304 Not Modified if ETag matches (browser already has this photo)
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .build();
        }

        try {
            PhotoService.PhotoData photoData = photoService.getPhoto(photoId);

            // Stream directly from GridFS to client without buffering in memory
            InputStreamResource resource = new InputStreamResource(photoData.inputStream());

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .eTag(etag)
                    .contentType(MediaType.parseMediaType(photoData.contentType()))
                    .contentLength(photoData.length())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + photoData.filename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get a thumbnail of a photo with HTTP caching support.
     * Thumbnails are generated on-demand and cached in GridFS.
     */
    @GetMapping("/photos/{photoId}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable String photoId,
                                                 @RequestParam(defaultValue = "80") int size,
                                                 @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
        // ETag includes both photo ID and size
        String etag = "\"thumb_" + photoId + "_" + size + "\"";

        // Return 304 Not Modified if ETag matches
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .build();
        }

        try {
            PhotoService.PhotoData thumbnailData = photoService.getThumbnail(photoId, size);

            // Stream thumbnail to client
            InputStreamResource resource = new InputStreamResource(thumbnailData.inputStream());

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                    .eTag(etag)
                    .contentType(MediaType.IMAGE_JPEG)
                    .contentLength(thumbnailData.length())
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a photo (generic endpoint, works for any visit's photo).
     * Supports htmx requests for seamless deletion.
     */
    @PostMapping("/photos/{photoId}/delete")
    public String deletePhoto(@PathVariable String photoId,
                             @RequestParam(required = false) String placeId,
                             @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                             RedirectAttributes redirectAttributes) {
        try {
            logger.debug("Deleting photo: {}, placeId: {}, htmx: {}", photoId, placeId, hxRequest);

            // Delete photo from GridFS
            photoService.deletePhoto(photoId);

            // If placeId provided, we need to remove the photo ID from the visit
            if (placeId != null && !placeId.isBlank()) {
                Place place = placeService.findById(placeId).orElse(null);
                if (place != null) {
                    // Find and update the visit that contains this photo
                    List<dk.placestracker.domain.model.Visit> updatedVisits = place.visits().stream()
                            .map(v -> {
                                if (v.photoIds() != null && v.photoIds().contains(photoId)) {
                                    return v.withRemovedPhoto(photoId);
                                }
                                return v;
                            })
                            .toList();

                    // Update place with modified visits
                    Place updatedPlace = place.withUpdate(
                            place.name(), place.location(), place.state(), place.country(),
                            updatedVisits, place.hasToilet(),
                            place.latitude(), place.longitude(),
                            place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                            place.googleRating(), place.googleReviewCount(),
                            java.time.LocalDateTime.now()
                    );
                    placeService.update(placeId, updatedPlace);
                }
            }

            // If htmx request, return empty content (htmx will swap out the element)
            if ("true".equals(hxRequest)) {
                logger.debug("Returning empty fragment for htmx delete");
                return "places/fragments/empty :: empty";
            }

            redirectAttributes.addFlashAttribute("success", "Photo deleted successfully!");
        } catch (Exception e) {
            if ("true".equals(hxRequest)) {
                return "places/fragments/empty :: empty";
            }
            redirectAttributes.addFlashAttribute("error", "Error deleting photo: " + e.getMessage());
        }

        // Redirect back to edit page if placeId was provided
        if (placeId != null && !placeId.isBlank()) {
            return "redirect:/places/" + placeId + "/edit";
        }
        return "redirect:/places";
    }

    /**
     * Helper method to filter out missing photos from visits.
     * This prevents broken image links when photos have been deleted from GridFS
     * but the photoId references still exist in the visit objects.
     * Uses batch query to avoid N+1 problem (1 query instead of N).
     */
    private Place cleanupMissingPhotos(Place place) {
        if (place == null || place.visits() == null || place.visits().isEmpty()) {
            return place;
        }

        // Collect all photo IDs from all visits in a single set
        Set<String> allPhotoIds = place.visits().stream()
                .filter(v -> v.photoIds() != null)
                .flatMap(v -> v.photoIds().stream())
                .collect(Collectors.toSet());

        // If no photos, return as-is
        if (allPhotoIds.isEmpty()) {
            return place;
        }

        // Single batch query to find all existing photos
        Set<String> existingPhotoIds = photoService.photosExist(allPhotoIds);

        // Filter visits using the cached result
        List<dk.placestracker.domain.model.Visit> cleanedVisits = place.visits().stream()
                .map(visit -> {
                    if (visit.photoIds() == null || visit.photoIds().isEmpty()) {
                        return visit;
                    }

                    // Filter out photoIds that don't exist in GridFS
                    List<String> validPhotoIds = visit.photoIds().stream()
                            .filter(existingPhotoIds::contains)
                            .collect(Collectors.toList());

                    // If the list changed, log a warning
                    if (validPhotoIds.size() < visit.photoIds().size()) {
                        int removed = visit.photoIds().size() - validPhotoIds.size();
                        logger.warn("Filtered out {} missing photo(s) from visit {} in place {}",
                                removed, visit.id(), place.id());
                    }

                    // Return visit with cleaned photo IDs
                    return visit.withPhotos(validPhotoIds);
                })
                .collect(Collectors.toList());

        // Return place with cleaned visits (preserve googleReviews and cached distance)
        return new Place(
                place.id(), place.name(), place.location(), place.state(), place.country(),
                cleanedVisits, place.hasToilet(),
                place.latitude(), place.longitude(),
                place.formattedAddress(), place.googlePlaceId(), place.website(), place.phoneNumber(),
                place.googleRating(), place.googleReviewCount(), place.googleReviews(),
                place.createdAt(), place.updatedAt(),
                place.drivingDistanceMiles(), place.drivingDurationMinutes(),
                place.distanceCalculatedAt(), place.distanceFromHomeLatLng(),
                place.favorite(), place.status()
        );
    }

    /**
     * Parse Google reviews from JSON string sent from the frontend.
     */
    private List<dk.placestracker.domain.model.Review> parseGoogleReviews(String googleReviewsJson) {
        if (googleReviewsJson == null || googleReviewsJson.isBlank()) {
            return new ArrayList<>();
        }

        try {
            // Use the injected ObjectMapper (thread-safe, configured by Spring Boot)
            List<Map<String, Object>> reviewMaps = objectMapper.readValue(
                    googleReviewsJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            List<dk.placestracker.domain.model.Review> reviews = new ArrayList<>();
            for (Map<String, Object> reviewMap : reviewMaps) {
                String authorName = (String) reviewMap.get("authorName");
                Integer rating = reviewMap.get("rating") != null ? ((Number) reviewMap.get("rating")).intValue() : null;
                String text = (String) reviewMap.get("text");
                String relativeTime = (String) reviewMap.get("relativeTime");
                String profilePhotoUrl = (String) reviewMap.get("profilePhotoUrl");

                LocalDateTime publishedAt = null;
                if (reviewMap.get("publishedAt") != null) {
                    // Handle different date formats
                    Object publishedAtObj = reviewMap.get("publishedAt");
                    if (publishedAtObj instanceof String) {
                        publishedAt = LocalDateTime.parse((String) publishedAtObj);
                    } else if (publishedAtObj instanceof List) {
                        // Jackson may deserialize as array [year, month, day, hour, minute, second, nano]
                        List<Integer> dateArray = (List<Integer>) publishedAtObj;
                        publishedAt = LocalDateTime.of(
                                dateArray.get(0), dateArray.get(1), dateArray.get(2),
                                dateArray.get(3), dateArray.get(4), dateArray.size() > 5 ? dateArray.get(5) : 0
                        );
                    }
                }

                reviews.add(dk.placestracker.domain.model.Review.create(
                        authorName, rating, text, relativeTime, profilePhotoUrl, publishedAt
                ));
            }

            return reviews;
        } catch (Exception e) {
            logger.error("Error parsing Google reviews JSON: {}", googleReviewsJson, e);
            return new ArrayList<>();
        }
    }

    /**
     * Parse and update visit durations from form input.
     * Duration strings are expected in format: "10min", "1h25min", "2d5h30min"
     *
     * @param visits List of visits to update with parsed durations
     * @param durationStrings Array of duration strings from form (one per visit)
     * @param model Model to add error messages
     * @return Updated list of visits with parsed durations, or original visits if parsing fails
     */
    /**
     * Extracts indexed parameters from the request (e.g., visitDurations[0], visitDurations[1], etc.).
     * Spring's @RequestParam doesn't automatically map indexed bracket syntax to arrays.
     */
    private String[] extractIndexedParameters(jakarta.servlet.http.HttpServletRequest request, String paramPrefix, int count) {
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            String paramName = paramPrefix + "[" + i + "]";
            result[i] = request.getParameter(paramName);
        }
        return result;
    }

    private List<dk.placestracker.domain.model.Visit> parseVisitDurations(
            List<dk.placestracker.domain.model.Visit> visits,
            String[] durationStrings,
            Model model) {

        logger.debug("Parsing durations - visits count: {}, duration strings: {}",
            visits != null ? visits.size() : 0,
            durationStrings != null ? java.util.Arrays.toString(durationStrings) : "null");

        if (durationStrings == null || durationStrings.length == 0) {
            logger.debug("No duration strings provided, returning original visits");
            return visits;
        }

        List<dk.placestracker.domain.model.Visit> updatedVisits = new ArrayList<>();

        for (int i = 0; i < visits.size(); i++) {
            dk.placestracker.domain.model.Visit visit = visits.get(i);
            logger.debug("Processing visit {}: date={}, currentDuration={}",
                i, visit.date(), visit.duration());

            // Get duration string for this visit index (if exists)
            if (i < durationStrings.length) {
                String durationStr = durationStrings[i];

                // Parse duration if provided (optional field)
                if (durationStr != null && !durationStr.isBlank()) {
                    try {
                        java.time.Duration duration = DurationUtils.parse(durationStr);
                        logger.debug("Successfully parsed duration '{}' to: {}", durationStr, duration);
                        // Update visit with parsed duration
                        visit = visit.withUpdate(visit.date(), visit.temperatureF(), visit.notes(), duration);
                        logger.debug("Updated visit {} with duration: {}", i, visit.duration());
                    } catch (IllegalArgumentException e) {
                        // Add error message for this specific visit
                        model.addAttribute("error",
                            "Invalid duration format for visit " + (i + 1) + ": " + e.getMessage());
                        logger.warn("Failed to parse duration '{}' for visit {}: {}",
                            durationStr, i + 1, e.getMessage());
                        // Keep visit without duration on error
                    }
                } else {
                    logger.debug("Duration string for visit {} is empty or null: '{}'", i, durationStr);
                }
            }

            updatedVisits.add(visit);
        }

        logger.debug("Returning {} updated visits", updatedVisits.size());
        return updatedVisits;
    }
}
