package de.openshopfront.platform.commerce;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Lets a Super Admin maintain the product catalog. */
@RestController
@RequestMapping("/api/v1/admin/commerce/products")
public class ProductAdminController {
  private final CatalogService catalog;

  public ProductAdminController(CatalogService catalog) {
    this.catalog = catalog;
  }

  public record SaveRequest(
      String name,
      String description,
      List<String> imageUrls,
      Boolean active,
      List<CatalogService.VariantInput> variants) {}

  /** Lists products with their purchasable option count. */
  @GetMapping
  public List<CatalogService.Product> list() {
    return catalog.list(false);
  }

  /** Loads one product with all variants and stock for the editor. */
  @GetMapping("/{slugOrId}")
  public CatalogService.AdminDetail detail(@PathVariable String slugOrId) {
    return catalog.detail(slugOrId);
  }

  /** Deletes a product and its variants. Orders keep their stored snapshots. */
  @DeleteMapping("/{slugOrId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable String slugOrId) {
    catalog.delete(slugOrId);
  }

  /** Creates a product and its first variant. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CatalogService.Product create(@RequestBody CreateRequest request) {
    return catalog.create(
        request.slug(),
        request.name(),
        request.description(),
        request.imageUrls(),
        request.variant());
  }

  /** Replaces product details and the full variant set. */
  @PutMapping("/{slug}")
  public CatalogService.Product update(
      @PathVariable String slug, @RequestBody SaveRequest request) {
    boolean active = request.active() == null || request.active();
    return catalog.update(
        slug,
        request.name(),
        request.description(),
        request.imageUrls(),
        active,
        request.variants());
  }

  /** Carries admin input for a new product. */
  public record CreateRequest(
      String slug,
      String name,
      String description,
      List<String> imageUrls,
      CatalogService.VariantInput variant) {}
}
