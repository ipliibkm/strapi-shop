package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ProductImageStorageTest {
  @TempDir java.nio.file.Path directory;

  @Test
  void storesAnImageWithAGeneratedPublicAddress() throws Exception {
    byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00};
    ProductImageStorage storage = new ProductImageStorage(directory.toString());

    ProductImageStorage.StoredImage result =
        storage.store(new MockMultipartFile("file", "photo.png", "image/png", png));

    assertThat(result.url()).matches("/api/v1/public/product-images/[0-9a-f-]{36}\\.png");
    try (var files = Files.list(directory)) {
      assertThat(files).hasSize(1);
    }
  }

  @Test
  void rejectsDataThatIsNotASupportedImage() {
    ProductImageStorage storage = new ProductImageStorage(directory.toString());

    assertThatThrownBy(
            () ->
                storage.store(
                    new MockMultipartFile("file", "note.txt", "text/plain", "test".getBytes())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Select a JPG, PNG, or WebP image up to 8 MB.");
  }
}
