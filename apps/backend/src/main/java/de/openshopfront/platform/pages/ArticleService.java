package de.openshopfront.platform.pages;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages articles that can be tagged to storefront pages. */
@Service
public class ArticleService {
  private final NamedParameterJdbcTemplate db;

  public ArticleService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Returns all articles for the administrator screen, newest first. */
  @Transactional(readOnly = true)
  public List<Article> listAll() {
    return db.query(
        """
        select id::text as id, title, slug, content_html, tags, published,
          created_at at time zone 'UTC' as created_at, updated_at at time zone 'UTC' as updated_at
        from article order by updated_at desc""",
        (rs, row) ->
            new Article(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("content_html"),
                List.of(rs.getString("tags").split(",")),
                rs.getBoolean("published"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  /** Returns one article by slug. */
  @Transactional(readOnly = true)
  public Article require(String slug) {
    List<Article> rows =
        db.query(
            """
            select id::text as id, title, slug, content_html, tags, published,
              created_at at time zone 'UTC' as created_at, updated_at at time zone 'UTC' as updated_at
            from article where slug=:slug""",
            Map.of("slug", slug),
            (rs, row) ->
                new Article(
                    rs.getString("id"),
                    rs.getString("title"),
                    rs.getString("slug"),
                    rs.getString("content_html"),
                    List.of(rs.getString("tags").split(",")),
                    rs.getBoolean("published"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
    if (rows.isEmpty()) throw new IllegalArgumentException("Article not found: " + slug);
    return rows.get(0);
  }

  /** Creates or updates an article from the administrator form. */
  @Transactional
  public Article save(String title, String slug, String contentHtml, List<String> tags) {
    Instant now = Instant.now();
    String tagCsv = String.join(",", tags == null ? List.of() : tags);
    Integer updated =
        db.update(
            """
            update article set title=:title, content_html=:content, tags=:tags, updated_at=:now where slug=:slug""",
            Map.of(
                "title",
                title.trim(),
                "content",
                contentHtml,
                "tags",
                tagCsv,
                "now",
                now,
                "slug",
                slug));
    if (updated == 0) {
      db.update(
          """
          insert into article (id,title,slug,content_html,tags,published,created_at,updated_at)
          values (:id,:title,:slug,:content,:tags,false,:now,:now)""",
          Map.of(
              "id",
              UUID.randomUUID(),
              "title",
              title.trim(),
              "slug",
              slug,
              "content",
              contentHtml,
              "tags",
              tagCsv,
              "now",
              now));
    }
    return require(slug);
  }

  /** Deletes an article by slug. */
  @Transactional
  public void delete(String slug) {
    db.update("delete from article where slug=:slug", Map.of("slug", slug));
  }

  /** Returns published articles tagged for one page slug. */
  @Transactional(readOnly = true)
  public List<Article> byPageSlug(String pageSlug) {
    return db.query(
        """
        select id::text as id, title, slug, content_html, tags, published,
          created_at at time zone 'UTC' as created_at, updated_at at time zone 'UTC' as updated_at
        from article where published=true and (','||tags||',') like ('%,'||:slug||',%')""",
        Map.of("slug", pageSlug),
        (rs, row) ->
            new Article(
                rs.getString("id"),
                rs.getString("title"),
                rs.getString("slug"),
                rs.getString("content_html"),
                List.of(rs.getString("tags").split(",")),
                rs.getBoolean("published"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  /** One article with its content and page tags. */
  public record Article(
      String id,
      String title,
      String slug,
      String contentHtml,
      List<String> tags,
      boolean published,
      Instant createdAt,
      Instant updatedAt) {}
}
