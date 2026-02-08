package dk.placestracker.web.controller;

import dk.placestracker.domain.model.Settings;
import dk.placestracker.service.PlaceService;
import dk.placestracker.service.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the home page.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
public class HomeController {

    private final PlaceService placeService;
    private final SettingsService settingsService;

    public HomeController(PlaceService placeService, SettingsService settingsService) {
        this.placeService = placeService;
        this.settingsService = settingsService;
    }

    @GetMapping("/")
    public String home(Model model) {
        Settings settings = settingsService.getSettings();
        var recentPlaces = placeService.findAllVisitedByMostRecentVisit().stream().limit(6).toList();
        long totalVisits = placeService.findAllVisited().stream()
            .mapToLong(place -> place.visits() != null ? place.visits().size() : 0)
            .sum();
        model.addAttribute("totalPlaces", placeService.findAllVisited().size());
        model.addAttribute("totalVisits", totalVisits);
        model.addAttribute("recentPlaces", recentPlaces);
        model.addAttribute("wishlistCount", placeService.countWishlist());
        model.addAttribute("favoritesCount", placeService.countFavorites());
        model.addAttribute("homeLocationSet", settings.hasHomeLocation());
        return "index";
    }
}
