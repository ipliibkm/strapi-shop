package de.openshopfront.platform.page;

import de.openshopfront.platform.pages.ArticleService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Serves published storefront page content to anonymous customers. */
@RestController
@RequestMapping("/api/v1/public/pages")
public class PublicPageController {
  private final PageService pages;
  private final ArticleService articles;

  public PublicPageController(PageService pages, ArticleService articles) {
    this.pages = pages;
    this.articles = articles;
  }

  /** Returns the approved blocks of one published page. */
  @GetMapping("/{slug}")
  public PageService.Page load(@PathVariable String slug) {
    return pages.published(slug);
  }

  /** Lists the enabled pages used by storefront navigation. */
  @GetMapping
  public List<PageService.PublicPageSummary> enabledPages() {
    return pages.enabledPages();
  }

  /** Returns published articles tagged for one page slug (e.g., "shop", "services"). */
  @GetMapping("/articles/{pageSlug}")
  public List<ArticleService.Article> articlesForPage(@PathVariable String pageSlug) {
    return articles.byPageSlug(pageSlug);
  }

  /** Maps an unknown or unpublished page to a readable client error. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "VALIDATION_ERROR", "message", error.getMessage()));
  }
}
