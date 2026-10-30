package de.openshopfront.platform.commerce;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manages products and purchasable variants with prices in minor units. */
@Service
public class CatalogService {
  private final NamedParameterJdbcTemplate db;

  public CatalogService(NamedParameterJdbcTemplate db) {
    this.db = db;
  }

  /** Lists products for the storefront and the admin. */
  @Transactional(readOnly = true)
  public List<Product> list(boolean activeOnly) {
    String where = activeOnly ? " where p.active=true" : "";
    List<Product> products =
        db.query(
            """
        select p.id,p.slug,p.name,p.description,coalesce(p.image_url,''),(select count(*) from product_variant v
             where v.product_id=p.id and v.active=true) as variant_count
          from product p"""
                + where
                + " order by p.name",
            Map.of(),
            (rs, row) ->
                new Product(
                    rs.getObject("id", UUID.class),
                    rs.getString("slug"),
                    rs.getString("name"),
                    rs.getString("description") == null ? "" : rs.getString("description"),
                    rs.getString(5),
                    List.of(),
                    rs.getInt("variant_count")));
    return products.stream().map(this::withImages).toList();
  }

  /** Loads one product with all active variants. */
  @Transactional(readOnly = true)
  public ProductDetail find(String slugOrId) {
    String clause = isUuid(slugOrId) ? "p.id::text=lower(:key)" : "p.slug=:key";
    List<Product> products =
        db.query(
            "select p.id,p.slug,p.name,coalesce(p.description,''),coalesce(p.image_url,'') from product p where "
                + clause
                + " and p.active=true",
            Map.of("key", slugOrId),
            (rs, row) ->
                new Product(
                    rs.getObject("id", UUID.class),
                    rs.getString("slug"),
                    rs.getString("name"),
                    rs.getString(4),
                    rs.getString(5),
                    List.of(),
                    0));
    if (products.isEmpty()) throw new IllegalArgumentException("This product is not available.");
    Product product = withImages(products.get(0));
    List<Variant> variants =
        db.query(
            "select v.id,coalesce(v.label,''),v.price_cents,v.currency,(v.stock_quantity - v.reserved_quantity) as available"
                + " from product_variant v where v.product_id=:id and v.active=true order by v.sku",
            Map.of("id", product.id()),
            (rs, row) ->
                new Variant(
                    rs.getObject("id", UUID.class),
                    rs.getString(2),
                    rs.getLong("price_cents"),
                    rs.getString("currency"),
                    Math.max(0, rs.getInt("available"))));
    if (variants.isEmpty())
      throw new IllegalArgumentException("This product has no purchasable options.");
    return new ProductDetail(product, List.copyOf(variants));
  }

  /** Loads one product with all variants, including inactive ones, for the admin editor. */
  @Transactional(readOnly = true)
  public AdminDetail detail(String slugOrId) {
    String clause = isUuid(slugOrId) ? "p.id::text=lower(:key)" : "p.slug=:key";
    List<Product> products =
        db.query(
            "select p.id,p.slug,p.name,coalesce(p.description,''),coalesce(p.image_url,'') from product p where "
                + clause,
            Map.of("key", slugOrId),
            (rs, row) ->
                new Product(
                    rs.getObject("id", UUID.class),
                    rs.getString("slug"),
                    rs.getString("name"),
                    rs.getString(4),
                    rs.getString(5),
                    List.of(),
                    0));
    if (products.isEmpty()) throw new IllegalArgumentException("This product does not exist.");
    Product product = withImages(products.get(0));
    List<VariantAdmin> variants =
        db.query(
            "select v.id,v.sku,coalesce(v.label,''),v.price_cents,v.stock_quantity from product_variant v where v.product_id=:id order by v.sku",
            Map.of("id", product.id()),
            (rs, row) ->
                new VariantAdmin(
                    rs.getObject("id", UUID.class),
                    rs.getString("sku"),
                    rs.getString(3),
                    rs.getLong("price_cents"),
                    rs.getInt("stock_quantity")));
    return new AdminDetail(product, List.copyOf(variants));
  }

  /** Deletes a product and its variants. Orders keep their stored snapshots. */
  @Transactional
  public void delete(String slugOrId) {
    UUID id = requireId(slugOrId);
    requireNoReservations(id);
    db.update("delete from product_variant where product_id=:id", Map.of("id", id));
    int changed = db.update("delete from product where id=:id", Map.of("id", id));
    if (changed == 0) throw new IllegalArgumentException("This product does not exist.");
  }

  /** Creates a product and its first variant. */
  @Transactional
  public Product create(
      String slug,
      String name,
      String description,
      List<String> imageUrls,
      VariantInput firstVariant) {
    String cleanSlug = validateSlug(slug);
    var variant = validateVariant(firstVariant);
    List<String> cleanImages = validateImageUrls(imageUrls);
    UUID id = UUID.randomUUID();
    db.update(
        "insert into product (id,slug,name,description,image_url,active,created_at,updated_at) values (:id,:slug,:name,:description,:image,true,now(),now())",
        Map.of(
            "id", id,
            "slug", cleanSlug,
            "name", name,
            "description", description,
            "image", cleanImages.isEmpty() ? "" : cleanImages.get(0)));
    replaceImages(id, cleanImages);
    insertVariant(id, variant);
    return list(false).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  /** Replaces product details and its full variant set. */
  @Transactional
  public Product update(
      String slug,
      String name,
      String description,
      List<String> imageUrls,
      boolean active,
      List<VariantInput> variants) {
    UUID id = requireId(slug);
    if (name == null || name.strip().isEmpty() || name.strip().length() > 200)
      throw new IllegalArgumentException("Enter a product name with no more than 200 characters.");
    String cleanDescription = description == null ? "" : description.replace("\u0000", "");
    if (cleanDescription.length() > 5_000)
      throw new IllegalArgumentException("Shorten the product description to 5000 characters.");
    List<String> cleanImages = validateImageUrls(imageUrls);
    requireNoReservations(id);
    db.update(
        "update product set name=:name,description=:description,image_url=:image,active=:active,updated_at=now() where id=:id",
        Map.of(
            "id", id,
            "name", name.strip(),
            "description", cleanDescription,
            "image", cleanImages.isEmpty() ? "" : cleanImages.get(0),
            "active", active));
    replaceImages(id, cleanImages);
    List<VariantInput> cleaned = new ArrayList<>();
    if (variants != null)
      for (VariantInput variant : variants) cleaned.add(validateVariant(variant));
    db.update("delete from product_variant where product_id=:id", Map.of("id", id));
    for (VariantInput variant : cleaned) insertVariant(id, variant);
    return list(false).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
  }

  private void insertVariant(UUID productId, VariantInput input) {
    UUID id = UUID.randomUUID();
    int changed =
        db.update(
            """
            insert into product_variant (id,product_id,sku,label,price_cents,currency,stock_quantity,reserved_quantity,active)
            values (:id,:productId,:sku,:label,:price,'EUR',:stock,0,true)""",
            Map.of(
                "id", id,
                "productId", productId,
                "sku", input.sku(),
                "label", input.label(),
                "price", input.priceCents(),
                "stock", input.stockQuantity()));
    if (changed == 0) throw new IllegalArgumentException("This variant could not be saved.");
  }

  private Product withImages(Product product) {
    List<String> images =
        db.query(
            "select url from product_image where product_id=:id order by position",
            Map.of("id", product.id()),
            (rs, row) -> rs.getString(1));
    if (images.isEmpty() && !product.imageUrl().isBlank()) images = List.of(product.imageUrl());
    String primary = images.isEmpty() ? "" : images.get(0);
    return new Product(
        product.id(),
        product.slug(),
        product.name(),
        product.description(),
        primary,
        List.copyOf(images),
        product.variantCount());
  }

  private void replaceImages(UUID productId, List<String> images) {
    db.update("delete from product_image where product_id=:id", Map.of("id", productId));
    for (int position = 0; position < images.size(); position++) {
      db.update(
          "insert into product_image (product_id,position,url) values (:id,:position,:url)",
          Map.of("id", productId, "position", position, "url", images.get(position)));
    }
  }

  private void requireNoReservations(UUID productId) {
    Boolean reserved =
        db.queryForObject(
            "select exists(select 1 from product_variant where product_id=:id and reserved_quantity>0)",
            Map.of("id", productId),
            Boolean.class);
    if (Boolean.TRUE.equals(reserved))
      throw new IllegalArgumentException(
          "This product has reserved stock. Complete or cancel its orders before you change variants.");
  }

  private UUID requireId(String slugOrId) {
    String clause = isUuid(slugOrId) ? "id::text=lower(:key)" : "slug=:key";
    List<UUID> ids =
        db.query(
            "select id from product where " + clause,
            Map.of("key", slugOrId),
            (rs, row) -> rs.getObject(1, UUID.class));
    return ids.stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("This product does not exist."));
  }

  private static boolean isUuid(String value) {
    try {
      UUID.fromString(value);
      return true;
    } catch (IllegalArgumentException error) {
      return false;
    }
  }

  private String validateSlug(String slug) {
    if (slug == null || !slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || slug.length() > 200)
      throw new IllegalArgumentException(
          "Use a slug of lower-case letters, numbers, and hyphens, for example running-shoe.");
    if (db.queryForObject(
        "select exists (select 1 from product where slug=:slug)",
        Map.of("slug", slug),
        Boolean.class))
      throw new IllegalArgumentException("A product with this slug already exists.");
    return slug;
  }

  /** Keeps an image address empty or a safe https/relative URL. */
  private static String validateImageUrl(String value) {
    String clean = value == null ? "" : value.strip();
    if (clean.isEmpty()) return "";
    if (clean.length() > 500 || !(clean.startsWith("https://") || clean.startsWith("/")))
      throw new IllegalArgumentException(
          "Enter an image address that starts with https or /, up to 500 characters.");
    return clean;
  }

  private static List<String> validateImageUrls(List<String> values) {
    if (values == null) return List.of();
    if (values.size() > 8) throw new IllegalArgumentException("Add no more than 8 product images.");
    LinkedHashSet<String> unique = new LinkedHashSet<>();
    for (String value : values) {
      String clean = validateImageUrl(value);
      if (!clean.isEmpty()) unique.add(clean);
    }
    return List.copyOf(unique);
  }

  private VariantInput validateVariant(VariantInput input) {
    if (input == null) throw new IllegalArgumentException("Add at least one purchasable option.");
    String sku = input.sku() == null ? "" : input.sku().strip();
    if (sku.isEmpty() || sku.length() > 100)
      throw new IllegalArgumentException("Enter a variant SKU with no more than 100 characters.");
    if (input.priceCents() < 0 || input.priceCents() > 10_000_000L)
      throw new IllegalArgumentException("Enter a price between 0 and 100000.00.");
    if (input.stockQuantity() < 0 || input.stockQuantity() > 1_000_000)
      throw new IllegalArgumentException("Enter stock between 0 and 1000000 units.");
    String label = input.label() == null ? "" : input.label().strip();
    if (label.length() > 200)
      throw new IllegalArgumentException("Shorten the variant name to 200 characters.");
    return new VariantInput(
        sku, label.isEmpty() ? sku : label, input.priceCents(), input.stockQuantity());
  }

  /** Contains a product without its purchasable options. */
  public record Product(
      UUID id,
      String slug,
      String name,
      String description,
      String imageUrl,
      List<String> imageUrls,
      int variantCount) {}

  /** Contains one purchasable option with the amount currently available. */
  public record Variant(
      UUID id, String label, long priceCents, String currency, int availableQuantity) {}

  /** Contains a product and its active variants for purchase. */
  public record ProductDetail(Product product, List<Variant> variants) {}

  /** Carries one variant with full stock state for the admin editor. */
  public record VariantAdmin(
      UUID id, String sku, String label, long priceCents, int stockQuantity) {}

  /** Contains a product and all of its variants for editing. */
  public record AdminDetail(Product product, List<VariantAdmin> variants) {}

  /** Carries admin input for one variant before validation. */
  public record VariantInput(String sku, String label, long priceCents, int stockQuantity) {}
}
