package de.openshopfront.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;

class AdminSessionSerializationTest {
  @Test
  void serializesStrapiAdministratorSecurityContext() throws Exception {
    var authority = new SimpleGrantedAuthority("ROLE_SUPER_ADMIN");
    var user = new StrapiAdminPrincipal("1", "Administrator", "admin@example.com", true);
    var original =
        new SecurityContextImpl(
            UsernamePasswordAuthenticationToken.authenticated(user, null, List.of(authority)));

    assertThat(original.getAuthentication().getName()).isEqualTo("1");

    byte[] serialized;
    try (var bytes = new ByteArrayOutputStream();
        var output = new ObjectOutputStream(bytes)) {
      output.writeObject(original);
      serialized = bytes.toByteArray();
    }

    SecurityContextImpl restored;
    try (var input = new ObjectInputStream(new ByteArrayInputStream(serialized))) {
      restored = (SecurityContextImpl) input.readObject();
    }

    assertThat(restored.getAuthentication().getPrincipal()).isEqualTo(user);
    assertThat(restored.getAuthentication().getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_SUPER_ADMIN");
  }
}
