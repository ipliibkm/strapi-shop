package de.openshopfront.platform.adminui;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lets a Super Admin manage which interface languages the admin panel offers. */
@RestController
@RequestMapping("/api/v1/admin/ui-settings")
public class AdminUiSettingsController {
  private final AdminUiLanguageService languages;

  public AdminUiSettingsController(AdminUiLanguageService languages) {
    this.languages = languages;
  }

  /** Reads the current interface language switches. */
  @GetMapping("/languages")
  public List<AdminUiLanguageService.Language> load() {
    return languages.require();
  }

  /** Enables or disables interface languages while keeping at least one active. */
  @PutMapping("/languages")
  @ResponseStatus(HttpStatus.OK)
  public List<AdminUiLanguageService.Language> saveLanguages(
      @RequestBody List<AdminUiLanguageService.LanguageChange> changes) {
    return languages.setLanguages(changes);
  }
}
