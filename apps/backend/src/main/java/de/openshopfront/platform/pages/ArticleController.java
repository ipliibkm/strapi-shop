package de.openshopfront.platform.pages;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrator endpoints for creating, editing and deleting tagged articles. */
@RestController
@RequestMapping("/api/v1/admin/articles")
public class ArticleController {
  private final ArticleService articles;

  public ArticleController(ArticleService articles) {
    this.articles = articles;
  }

  /** Lists every article with its tags for the admin screen. */
  @GetMapping
  public List<ArticleService.Article> list() {
    return articles.listAll();
  }

  /** Returns one article by slug. */
  @GetMapping("/{slug}")
  public ArticleService.Article detail(@PathVariable String slug) {
    return articles.require(slug);
  }

  /** Creates or updates an article from the admin form. */
  @PutMapping("/{slug}")
  public ArticleService.Article save(@PathVariable String slug, @RequestBody SaveRequest body) {
    return articles.save(body.title(), slug, body.contentHtml(), body.tags());
  }

  /** Deletes an article by slug. */
  @DeleteMapping("/{slug}")
  public void delete(@PathVariable String slug) {
    articles.delete(slug);
  }

  /** The form payload for saving one article. */
  public record SaveRequest(String title, String contentHtml, List<String> tags) {}
}
