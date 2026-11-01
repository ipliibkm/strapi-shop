package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Verifies that checkout domain failures map to readable conflict responses. */
class PublicCommerceControllerTest {
  private final PublicCommerceController subject =
      new PublicCommerceController(
          mock(CatalogService.class),
          mock(OrderService.class),
          mock(CommerceSettingsService.class),
          mock(StripePaymentProvider.class),
          mock(OrderLookupRateLimiter.class),
          mock(CustomerAccountService.class));

  @SuppressWarnings("unchecked")
  private Map<String, String> body(ResponseEntity<?> response) {
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    return (Map<String, String>) response.getBody();
  }

  @Test
  void outOfStockNamesTheProductAndStatesTheNextStep() {
    OrderService.OutOfStockException error = new OrderService.OutOfStockException("Court Classic");

    Map<String, String> result = body(subject.outOfStock(error));

    assertThat(result).containsEntry("code", "OUT_OF_STOCK");
    assertThat(result.get("message"))
        .isEqualTo("Court Classic is out of stock. Select a lower quantity or another product.");
  }

  @Test
  void outOfStockWithoutProductNameStillReadsCorrectly() {
    OrderService.OutOfStockException error = new OrderService.OutOfStockException(null);

    assertThat(subject.outOfStock(error).getBody())
        .containsEntry(
            "message", "This product is out of stock. Select a lower quantity or another product.");
  }

  @Test
  void disabledShopReturnsTheReadableClosedMessage() {
    Map<String, String> result = body(subject.shopDisabled());

    assertThat(result).containsEntry("code", "SHOP_DISABLED");
    assertThat(result.get("message")).isEqualTo("The shop is not open yet.");
  }
}
