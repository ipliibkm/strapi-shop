package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CustomerAreaControllerTest {
  @Test
  void onlyOrdersBoundToTheVerifiedStrapiIdentityAreListed() {
    CustomerAccountService accounts = mock(CustomerAccountService.class);
    OrderService orders = mock(OrderService.class);
    when(accounts.requireCustomer("Bearer token"))
        .thenReturn(new CustomerAccountService.Customer(42L, "person@example.org", "person"));
    when(orders.customerOrders(42L)).thenReturn(List.of());

    CustomerAreaController controller =
        new CustomerAreaController(accounts, mock(CommerceSettingsService.class), orders);

    assertThat(controller.orders("Bearer token")).isEmpty();
    verify(orders).customerOrders(42L);
  }

  @Test
  void disabledAreaRejectsAccountOperations() {
    CommerceSettingsService settings = mock(CommerceSettingsService.class);
    when(settings.require())
        .thenReturn(new CommerceSettingsService.Settings(true, false, 0, 0, null));
    CustomerAccountService accounts = new CustomerAccountService("http://localhost:1337", settings);

    assertThatThrownBy(() -> accounts.requireCustomer("Bearer token"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(error -> ((ResponseStatusException) error).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }
}
