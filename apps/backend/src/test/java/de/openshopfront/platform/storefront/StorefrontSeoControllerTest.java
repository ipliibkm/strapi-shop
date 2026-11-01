package de.openshopfront.platform.storefront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.openshopfront.platform.commerce.CatalogService;
import de.openshopfront.platform.commerce.CommerceSettingsService;
import de.openshopfront.platform.page.PageService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Verifies host-specific crawler files and their public canonical URLs. */
class StorefrontSeoControllerTest {
  private final PageService pages = mock(PageService.class);
  private final CatalogService catalog = mock(CatalogService.class);
  private final CommerceSettingsService commerce = mock(CommerceSettingsService.class);
  private final StorefrontSeoController subject =
      new StorefrontSeoController(pages, catalog, commerce);
  private final MockHttpServletRequest request = new MockHttpServletRequest();

  @BeforeEach
  void configureRequest() {
    request.setScheme("http");
    request.setServerName("backend");
    request.addHeader("X-Forwarded-Proto", "https");
    request.addHeader("X-Forwarded-Host", "shop.example.com");
  }

  @Test
  void sitemapContainsEnabledPagesAndActiveProducts() {
    when(pages.enabledPages())
        .thenReturn(
            List.of(
                new PageService.PublicPageSummary("home", "Home"),
                new PageService.PublicPageSummary("about", "About")));
    when(commerce.require())
        .thenReturn(new CommerceSettingsService.Settings(true, false, 0, 0, null));
    when(catalog.list(true))
        .thenReturn(
            List.of(
                new CatalogService.Product(
                    UUID.randomUUID(), "green-chair", "Green chair", "", "", List.of(), 1)));

    String result = subject.sitemap(request);

    assertThat(result)
        .contains("<loc>https://shop.example.com/</loc>")
        .contains("<loc>https://shop.example.com/about</loc>")
        .contains("<loc>https://shop.example.com/products/green-chair</loc>");
  }

  @Test
  void robotsDisallowsPrivateCustomerPages() {
    assertThat(subject.robots(request))
        .isEqualTo(
            "User-agent: *\nDisallow: /cart\nDisallow: /order\nSitemap: https://shop.example.com/sitemap.xml\n");
  }
}
