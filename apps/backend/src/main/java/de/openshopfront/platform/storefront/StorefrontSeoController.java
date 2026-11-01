package de.openshopfront.platform.storefront;

import de.openshopfront.platform.commerce.CatalogService;
import de.openshopfront.platform.commerce.CommerceSettingsService;
import de.openshopfront.platform.page.PageService;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

/** Publishes crawler discovery files for the public storefront host. */
@RestController
public class StorefrontSeoController {
  private final PageService pages;
  private final CatalogService catalog;
  private final CommerceSettingsService commerce;

  public StorefrontSeoController(
      PageService pages, CatalogService catalog, CommerceSettingsService commerce) {
    this.pages = pages;
    this.catalog = catalog;
    this.commerce = commerce;
  }

  /** Lists indexable pages and active products with canonical storefront URLs. */
  @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
  public String sitemap(HttpServletRequest request) {
    String baseUrl = baseUrl(request);
    List<PageService.PublicPageSummary> enabledPages = pages.enabledPages();
    StringBuilder xml =
        new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
    boolean hasHome = false;
    for (PageService.PublicPageSummary page : enabledPages) {
      String slug = page.slug();
      if ("home".equals(slug)) hasHome = true;
      appendUrl(xml, baseUrl + ("home".equals(slug) ? "/" : "/" + pathSegment(slug)));
    }
    if (!hasHome) appendUrl(xml, baseUrl + "/");
    if (commerce.require().shopEnabled())
      for (CatalogService.Product product : catalog.list(true))
        appendUrl(xml, baseUrl + "/products/" + pathSegment(product.slug()));
    return xml.append("</urlset>").toString();
  }

  /** Allows public crawling and points search engines to the host-specific sitemap. */
  @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
  public String robots(HttpServletRequest request) {
    return "User-agent: *\nDisallow: /cart\nDisallow: /order\nSitemap: "
        + baseUrl(request)
        + "/sitemap.xml\n";
  }

  private static void appendUrl(StringBuilder xml, String location) {
    xml.append("<url><loc>").append(location).append("</loc></url>");
  }

  private static String pathSegment(String value) {
    return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
  }

  private static String baseUrl(HttpServletRequest request) {
    String forwardedProtocol = first(request.getHeader("X-Forwarded-Proto"));
    String protocol =
        "https".equalsIgnoreCase(forwardedProtocol) || "http".equalsIgnoreCase(forwardedProtocol)
            ? forwardedProtocol.toLowerCase()
            : request.getScheme();
    String forwardedHost = first(request.getHeader("X-Forwarded-Host"));
    String host =
        forwardedHost != null && forwardedHost.matches("[A-Za-z0-9.-]+(?::[0-9]{1,5})?")
            ? forwardedHost
            : request.getServerName();
    return protocol + "://" + host;
  }

  private static String first(String value) {
    if (value == null || value.isBlank()) return null;
    return value.split(",", 2)[0].strip();
  }
}
