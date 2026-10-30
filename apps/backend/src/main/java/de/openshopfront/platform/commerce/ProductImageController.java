package de.openshopfront.platform.commerce;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Uploads product images for administrators and serves them to storefront clients. */
@RestController
@RequestMapping("/api/v1")
public class ProductImageController {
  private final ProductImageStorage storage;

  public ProductImageController(ProductImageStorage storage) {
    this.storage = storage;
  }

  /** Stores one validated product image. */
  @PostMapping(
      path = "/admin/commerce/product-images",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProductImageStorage.StoredImage upload(@RequestPart("file") MultipartFile file) {
    return storage.store(file);
  }

  /** Returns one immutable uploaded product image. */
  @GetMapping("/public/product-images/{filename}")
  public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable String filename) {
    ProductImageStorage.StoredResource stored = storage.load(filename);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(stored.contentType()))
        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
        .header("X-Content-Type-Options", "nosniff")
        .body(stored.resource());
  }

  /** Maps invalid image data to a readable client error. */
  @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException error) {
    return ResponseEntity.badRequest()
        .body(Map.of("code", "VALIDATION_ERROR", "message", error.getMessage()));
  }
}
