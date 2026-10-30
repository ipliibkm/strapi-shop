package de.openshopfront.platform.page;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.openshopfront.platform.auth.StrapiAdminClient;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Stores storefront page drafts and published content in Strapi. */
@Service
public class PageService {
  private static final String UID = "api::storefront-page.storefront-page";
  private static final String CONTENT_MANAGER_PATH = "/content-manager/collection-types/" + UID;
  private static final Set<String> BLOCK_TYPES =
      Set.of("heading", "text", "image", "gallery", "cta", "products", "form");
  private static final List<String> PAGE_TARGETS =
      List.of("booking", "shop", "about", "contact", "services", "home");
  private static final int MAX_BLOCKS = 40;
  private static final int MAX_HTML_LENGTH = 20_000;

  private final StrapiAdminClient admin;
  private final RestClient publicApi;
  private final ObjectMapper json;

  public PageService(
      StrapiAdminClient admin,
      ObjectMapper json,
      @Value("${platform.strapi.base-url}") String baseUrl) {
    this.admin = admin;
    this.json = json;
    this.publicApi = RestClient.builder().baseUrl(baseUrl).build();
  }

  /** Lists the Strapi storefront page documents with their current states. */
  public List<PageSummary> list(HttpSession session) {
    JsonNode response =
        admin.content(
            session, "GET", CONTENT_MANAGER_PATH + "?page=1&pageSize=100&sort=slug:ASC", null);
    List<PageSummary> pages = new ArrayList<>();
    for (JsonNode item : response.path("results")) {
      String status = item.path("status").asText("draft");
      pages.add(
          new PageSummary(
              item.path("documentId").asText(),
              item.path("slug").asText(),
              item.path("title").asText(),
              item.path("enabled").asBoolean(true),
              !"draft".equals(status),
              "modified".equals(status)));
    }
    return List.copyOf(pages);
  }

  /** Loads one editable Strapi page with its draft blocks. */
  public Page find(String slug, HttpSession session) {
    return listDocuments(session).stream()
        .filter(page -> page.slug().equals(slug))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("This page does not exist."));
  }

  /** Saves a validated Strapi draft without changing the published version. */
  public Page save(
      String slug, String title, boolean enabled, List<JsonNode> blocks, HttpSession session) {
    Page current = find(slug, session);
    ObjectNode body = json.createObjectNode();
    body.put("title", validateTitle(title));
    body.put("slug", slug);
    body.put("enabled", enabled);
    ArrayNode cleanBlocks = body.putArray("blocks");
    normalizeBlocks(blocks).forEach(cleanBlocks::add);
    admin.content(session, "PUT", CONTENT_MANAGER_PATH + "/" + current.documentId(), body);
    return find(slug, session);
  }

  /** Publishes the current Strapi draft. */
  public Page publish(String slug, HttpSession session) {
    Page current = find(slug, session);
    admin.content(
        session,
        "POST",
        CONTENT_MANAGER_PATH + "/" + current.documentId() + "/actions/publish",
        json.createObjectNode());
    return find(slug, session);
  }

  /** Returns one enabled, published page from Strapi. */
  public Page published(String slug) {
    try {
      JsonNode item =
          publicApi
              .get()
              .uri("/api/storefront-pages/by-slug/{slug}", slug)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(JsonNode.class);
      if (item == null) throw new IllegalArgumentException("This page is not available.");
      return pageFrom(item, true);
    } catch (HttpClientErrorException.NotFound error) {
      throw new IllegalArgumentException("This page is not available.");
    }
  }

  /** Lists enabled page slugs for storefront navigation. */
  public List<PublicPageSummary> enabledPages() {
    JsonNode response =
        publicApi
            .get()
            .uri("/api/storefront-pages/enabled")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class);
    List<PublicPageSummary> pages = new ArrayList<>();
    if (response != null)
      for (JsonNode item : response)
        pages.add(new PublicPageSummary(item.path("slug").asText(), item.path("title").asText()));
    return List.copyOf(pages);
  }

  private List<Page> listDocuments(HttpSession session) {
    JsonNode response =
        admin.content(
            session, "GET", CONTENT_MANAGER_PATH + "?page=1&pageSize=100&sort=slug:ASC", null);
    List<Page> pages = new ArrayList<>();
    for (JsonNode item : response.path("results")) pages.add(pageFrom(item, false));
    return List.copyOf(pages);
  }

  private Page pageFrom(JsonNode item, boolean publicPage) {
    List<JsonNode> blocks = new ArrayList<>();
    if (item.path("blocks").isArray()) item.path("blocks").forEach(blocks::add);
    String status = item.path("status").asText(publicPage ? "published" : "draft");
    return new Page(
        item.path("documentId").asText(),
        item.path("slug").asText(),
        item.path("title").asText(),
        item.path("enabled").asBoolean(true),
        List.copyOf(blocks),
        !"draft".equals(status));
  }

  private List<JsonNode> normalizeBlocks(List<JsonNode> blocks) {
    if (blocks == null || blocks.isEmpty()) return List.of();
    if (blocks.size() > MAX_BLOCKS)
      throw new IllegalArgumentException("Use no more than " + MAX_BLOCKS + " sections per page.");
    List<JsonNode> clean = new ArrayList<>();
    for (JsonNode block : blocks) {
      String type = text(block, "type");
      if (!BLOCK_TYPES.contains(type))
        throw new IllegalArgumentException(
            "This section type is not supported: " + (type.isEmpty() ? "(none)" : type));
      ObjectNode value = json.createObjectNode().put("type", type);
      switch (type) {
        case "heading" ->
            value
                .put("eyebrow", limit(text(block, "eyebrow"), 120))
                .put("title", limit(text(block, "title"), 200));
        case "text" ->
            value.put(
                "html", RichTextSanitizer.sanitize(limit(text(block, "html"), MAX_HTML_LENGTH)));
        case "image" -> {
          String url = limit(text(block, "url"), 500);
          if (!url.isEmpty() && !isSafeUrl(url))
            throw new IllegalArgumentException(
                "Enter an image address that starts with https or /");
          value.put("url", url).put("caption", limit(text(block, "caption"), 200));
        }
        case "gallery" -> {
          ArrayNode items = json.createArrayNode();
          JsonNode rawItems = block.get("items");
          if (rawItems == null || !rawItems.isArray() || rawItems.isEmpty())
            throw new IllegalArgumentException("Add at least one image to this gallery.");
          if (rawItems.size() > 12)
            throw new IllegalArgumentException("Show no more than 12 images in a gallery.");
          for (JsonNode item : rawItems) {
            String url = limit(text(item, "url"), 500);
            if (!url.isEmpty() && !isSafeUrl(url))
              throw new IllegalArgumentException(
                  "Enter an image address that starts with https or /");
            items.add(
                json.createObjectNode()
                    .put("url", url)
                    .put("caption", limit(text(item, "caption"), 200)));
          }
          value.set("items", items);
        }
        case "cta" -> {
          String target = text(block, "target").toLowerCase(Locale.ROOT);
          if (!PAGE_TARGETS.contains(target))
            throw new IllegalArgumentException("The button must point to a known page.");
          value.put("label", limit(text(block, "label"), 80)).put("target", target);
        }
        case "products" -> {
          int max = block.path("limit").asInt(6);
          if (max < 1 || max > 24)
            throw new IllegalArgumentException("Show between 1 and 24 products.");
          value.put("limit", max);
        }
        case "form" -> {
          String formSlug = limit(text(block, "formSlug"), 64).toLowerCase(Locale.ROOT);
          if (!formSlug.matches("[a-z][a-z0-9_]{0,63}"))
            throw new IllegalArgumentException("Select a valid form for this section.");
          value.put("formSlug", formSlug);
        }
        default -> throw new IllegalStateException("Unexpected page section type.");
      }
      clean.add(value);
    }
    return List.copyOf(clean);
  }

  private static String text(JsonNode block, String field) {
    JsonNode value = block == null ? null : block.get(field);
    return value == null || !value.isTextual() ? "" : value.asText();
  }

  private static String limit(String value, int maxLength) {
    if (value == null || value.isEmpty()) return "";
    String clean = value.strip().replace("\u0000", "");
    return clean.length() > maxLength ? clean.substring(0, maxLength) : clean;
  }

  private static boolean isSafeUrl(String value) {
    return value.startsWith("https://") || value.startsWith("/");
  }

  private static String validateTitle(String title) {
    if (title == null || title.strip().isEmpty() || title.strip().length() > 200)
      throw new IllegalArgumentException("Enter a page title with no more than 200 characters.");
    return title.strip();
  }

  public record PageSummary(
      String documentId,
      String slug,
      String title,
      boolean enabled,
      boolean published,
      boolean unpublishedChanges) {}

  public record PublicPageSummary(String slug, String title) {}

  public record Page(
      String documentId,
      String slug,
      String title,
      boolean enabled,
      List<JsonNode> blocks,
      boolean published) {}
}
