package de.openshopfront.platform.commerce;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Stores validated product image files outside the application package. */
@Service
public class ProductImageStorage {
  private static final long MAX_BYTES = 8L * 1024 * 1024;
  private static final Map<String, String> EXTENSIONS =
      Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");
  private final Path directory;

  public ProductImageStorage(
      @Value("${platform.commerce.upload-directory:/app/uploads/products}") String directory) {
    this.directory = Path.of(directory).toAbsolutePath().normalize();
  }

  /** Validates and stores one JPG, PNG, or WebP image. */
  public StoredImage store(MultipartFile file) {
    try {
      byte[] bytes = file.getBytes();
      String contentType = detectType(bytes);
      if (bytes.length == 0 || bytes.length > MAX_BYTES || contentType == null)
        throw new IllegalArgumentException("Select a JPG, PNG, or WebP image up to 8 MB.");
      Files.createDirectories(directory);
      String filename = UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
      Files.write(directory.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
      return new StoredImage("/api/v1/public/product-images/" + filename);
    } catch (IOException error) {
      throw new IllegalStateException("The image could not be stored.", error);
    }
  }

  /** Loads one stored image after validating its generated filename. */
  public StoredResource load(String filename) {
    if (filename == null || !filename.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)"))
      throw new IllegalArgumentException("This image does not exist.");
    Path file = directory.resolve(filename).normalize();
    if (!file.startsWith(directory) || !Files.isRegularFile(file))
      throw new IllegalArgumentException("This image does not exist.");
    try {
      String extension = filename.substring(filename.lastIndexOf('.') + 1);
      String contentType =
          switch (extension) {
            case "jpg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            default -> "image/webp";
          };
      return new StoredResource(new UrlResource(file.toUri()), contentType);
    } catch (java.net.MalformedURLException error) {
      throw new IllegalArgumentException("This image does not exist.", error);
    }
  }

  private static String detectType(byte[] bytes) {
    if (bytes.length >= 3
        && (bytes[0] & 0xff) == 0xff
        && (bytes[1] & 0xff) == 0xd8
        && (bytes[2] & 0xff) == 0xff) return MediaType.IMAGE_JPEG_VALUE;
    if (bytes.length >= 8
        && (bytes[0] & 0xff) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4e
        && bytes[3] == 0x47
        && bytes[4] == 0x0d
        && bytes[5] == 0x0a
        && bytes[6] == 0x1a
        && bytes[7] == 0x0a) return MediaType.IMAGE_PNG_VALUE;
    if (bytes.length >= 12
        && bytes[0] == 'R'
        && bytes[1] == 'I'
        && bytes[2] == 'F'
        && bytes[3] == 'F'
        && bytes[8] == 'W'
        && bytes[9] == 'E'
        && bytes[10] == 'B'
        && bytes[11] == 'P') return "image/webp";
    return null;
  }

  public record StoredImage(String url) {}

  public record StoredResource(Resource resource, String contentType) {}
}
