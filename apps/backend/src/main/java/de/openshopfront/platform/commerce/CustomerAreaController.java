package de.openshopfront.platform.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Exposes only customer account actions and orders owned by the Strapi user. */
@RestController
@RequestMapping("/api/v1/public/customer-area")
public class CustomerAreaController {
  private final CustomerAccountService accounts;
  private final CommerceSettingsService settings;
  private final OrderService orders;

  public CustomerAreaController(
      CustomerAccountService accounts, CommerceSettingsService settings, OrderService orders) {
    this.accounts = accounts;
    this.settings = settings;
    this.orders = orders;
  }

  @GetMapping
  public Map<String, Boolean> enabled() {
    return Map.of("enabled", settings.require().customerAreaEnabled());
  }

  @PostMapping("/register")
  public JsonNode register(@RequestBody Credentials credentials) {
    validate(credentials);
    return accounts.authenticate(
        "/api/auth/local/register",
        Map.of(
            "username",
            credentials.email(),
            "email",
            credentials.email(),
            "password",
            credentials.password()));
  }

  @PostMapping("/login")
  public JsonNode login(@RequestBody Credentials credentials) {
    validate(credentials);
    return accounts.authenticate(
        "/api/auth/local",
        Map.of("identifier", credentials.email(), "password", credentials.password()));
  }

  @GetMapping("/me")
  public CustomerAccountService.Customer me(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    return accounts.requireCustomer(authorization);
  }

  @GetMapping("/orders")
  public List<PublicOrder> orders(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    CustomerAccountService.Customer customer = accounts.requireCustomer(authorization);
    return orders.customerOrders(customer.id()).stream()
        .map(
            order ->
                new PublicOrder(
                    order.orderNumber(),
                    order.status(),
                    order.totalCents(),
                    order.currency(),
                    order.createdAt()))
        .toList();
  }

  public record Credentials(String email, String password) {}

  private static void validate(Credentials credentials) {
    if (credentials == null
        || credentials.email() == null
        || credentials.password() == null
        || credentials.email().length() > 254
        || credentials.password().length() > 1024
        || credentials.email().isBlank()
        || credentials.password().isBlank())
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter an email and password.");
  }

  public record PublicOrder(
      String orderNumber,
      String status,
      long totalCents,
      String currency,
      java.time.Instant createdAt) {}
}
