package dk.placestracker.web.controller;

import dk.placestracker.domain.model.Settings;
import dk.placestracker.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for application settings management with htmx support.
 *
 * @author Dmitriy Kopylenko
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public String showSettings(Model model) {
        Settings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        return "settings/index";
    }

    @PostMapping
    public String updateSettings(@Valid @ModelAttribute("settings") Settings settings,
                                BindingResult bindingResult,
                                @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("settings", settings);
            if ("true".equals(hxRequest)) {
                // Return form fragment with errors
                return "settings/fragments :: form-section";
            }
            return "settings/index";
        }

        Settings updatedSettings = settingsService.updateSettings(settings.homeLatitude(), settings.homeLongitude());
        model.addAttribute("settings", updatedSettings);

        if ("true".equals(hxRequest)) {
            // Return updated fragments for htmx
            model.addAttribute("success", "Home location updated successfully");
            // Use HX-Trigger to show success message and update current location card
            return "settings/fragments :: form-section";
        }

        redirectAttributes.addFlashAttribute("success", "Home location updated successfully");
        return "redirect:/settings";
    }

    @PostMapping("/clear")
    public String clearHomeLocation(@RequestHeader(value = "HX-Request", required = false) String hxRequest,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        Settings updatedSettings = settingsService.clearHomeLocation();
        model.addAttribute("settings", updatedSettings);

        if ("true".equals(hxRequest)) {
            // Return empty current location card (will be hidden since no location set)
            return "settings/fragments :: current-location";
        }

        redirectAttributes.addFlashAttribute("success", "Home location cleared successfully");
        return "redirect:/settings";
    }

    /**
     * Get current location card fragment (for htmx updates).
     */
    @GetMapping("/current-location")
    public String getCurrentLocationFragment(Model model) {
        Settings settings = settingsService.getSettings();
        model.addAttribute("settings", settings);
        return "settings/fragments :: current-location";
    }
}
