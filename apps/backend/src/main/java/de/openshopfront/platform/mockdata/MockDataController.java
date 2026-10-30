package de.openshopfront.platform.mockdata;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Applies test store profiles so the whole application can be tested with realistic data. */
@RestController
@RequestMapping("/api/v1/admin/mock-data")
public class MockDataController {
  private final MockDataService mockData;

  public MockDataController(MockDataService mockData) {
    this.mockData = mockData;
  }

  /** The supported test store keys, in display order. */
  @GetMapping("/profiles")
  public List<String> profiles() {
    return MockDataService.profileKeys();
  }

  /** Replaces business profile, catalog, booking services and pages with one test store. */
  @PostMapping("/{profile}")
  public MockDataService.Summary apply(@PathVariable String profile, HttpSession session) {
    return mockData.apply(profile, session);
  }
}
