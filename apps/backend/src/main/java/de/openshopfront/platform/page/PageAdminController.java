package de.openshopfront.platform.page;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lets a Super Admin edit, preview, and publish storefront pages. */
@RestController
@RequestMapping("/api/v1/admin/pages")
public class PageAdminController {
  private final PageService pages;
  private final String storefrontBaseUrl;

  public PageAdminController(
      PageService pages, @Value("${platform.storefront-base-url}") String storefrontBaseUrl) {
    this.pages = pages;
    this.storefrontBaseUrl = storefrontBaseUrl;
  }

  public record SaveRequest(String title, boolean enabled, List<JsonNode> blocks) {}

  /** Lists all editable storefront pages. */
  @GetMapping
  public List<PageService.PageSummary> list(HttpSession session) {
    return pages.list(session);
  }

  /** Loads one page draft for the WYSIWYG editor. */
  @GetMapping("/{slug}")
  public PageService.Page load(@PathVariable String slug, HttpSession session) {
    return pages.find(slug, session);
  }

  /** Saves a validated and sanitized draft without publishing it. */
  @PutMapping("/{slug}")
  public PageService.Page save(
      @PathVariable String slug, @RequestBody SaveRequest request, HttpSession session) {
    return pages.save(slug, request.title(), request.enabled(), request.blocks(), session);
  }

  /** Publishes the current draft to the storefront. */
  @PostMapping("/{slug}/publish")
  @ResponseStatus(HttpStatus.OK)
  public PageService.Page publish(@PathVariable String slug, HttpSession session) {
    return pages.publish(slug, session);
  }

  /** Returns the real storefront address used by the page preview frame. */
  @GetMapping("/preview-config")
  public Map<String, String> previewConfig() {
    return Map.of("storefrontBaseUrl", storefrontBaseUrl);
  }
}
