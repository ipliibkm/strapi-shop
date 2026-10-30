package de.openshopfront.platform.commerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class CommerceSettingsServiceTest {
  @Test
  @SuppressWarnings("unchecked")
  void savesNullWhenFreeShippingHasNoThreshold() {
    NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    var saved = new CommerceSettingsService.Settings(true, 1900, 499, null);
    when(db.query(startsWith("select"), anyMap(), any(RowMapper.class))).thenReturn(List.of(saved));

    CommerceSettingsService.Settings result =
        new CommerceSettingsService(db).update(true, 1900, 499, null);

    ArgumentCaptor<Map<String, ?>> parameters = ArgumentCaptor.forClass(Map.class);
    verify(db).update(startsWith("update commerce_settings"), parameters.capture());
    assertThat(parameters.getValue()).containsEntry("threshold", null);
    assertThat(result).isEqualTo(saved);
  }
}
