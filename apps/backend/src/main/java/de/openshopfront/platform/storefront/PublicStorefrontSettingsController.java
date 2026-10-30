package de.openshopfront.platform.storefront;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serves the storefront language switches and published palette to anonymous visitors. */
@RestController
@RequestMapping("/api/v1/public/settings")
public class PublicStorefrontSettingsController {
  private final StorefrontSettingsService settings;

  public PublicStorefrontSettingsController(StorefrontSettingsService settings) {
    this.settings = settings;
  }

  /** Returns the enabled languages and the published color palette. */
  @GetMapping
  public StorefrontSettingsService.Settings load() {
    return settings.require();
  }
}
