package de.openshopfront.platform.storefront;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lets a Super Admin manage storefront languages and the published color palette. */
@RestController
@RequestMapping("/api/v1/admin/storefront-settings")
public class AdminStorefrontSettingsController {
  private final StorefrontSettingsService settings;

  public AdminStorefrontSettingsController(StorefrontSettingsService settings) {
    this.settings = settings;
  }

  /** Reads the current language switches and published palette. */
  @GetMapping
  public StorefrontSettingsService.Settings load() {
    return settings.require();
  }

  /** Enables or disables website languages while keeping at least one active. */
  @PutMapping("/languages")
  @ResponseStatus(HttpStatus.OK)
  public StorefrontSettingsService.Settings saveLanguages(
      @RequestBody List<StorefrontSettingsService.LanguageChange> changes) {
    return settings.setLanguages(changes);
  }

  /** Publishes the selected color palette to every storefront visitor. */
  @PutMapping("/appearance")
  @ResponseStatus(HttpStatus.OK)
  public StorefrontSettingsService.Settings saveAppearance(@RequestBody AppearanceRequest request) {
    return settings.setTheme(request.theme());
  }

  /** Publishes a custom color palette, or clears it when the body is empty. */
  @PutMapping("/custom-palette")
  @ResponseStatus(HttpStatus.OK)
  public StorefrontSettingsService.Settings saveCustomPalette(
      @RequestBody(required = false) CustomPaletteRequest request) {
    if (request == null || request.palette() == null) return settings.setCustomPalette(null);
    return settings.setCustomPalette(request.palette().toString());
  }

  public record AppearanceRequest(String theme) {}

  /** Carries the custom palette as a JSON object. */
  public record CustomPaletteRequest(com.fasterxml.jackson.databind.JsonNode palette) {}
}
