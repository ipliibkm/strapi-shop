package de.ipli.platform.setup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

class SetupControllerUnitTest {
 @Test void rejectsWrongTokenBeforeDatabaseWrites(){
  NamedParameterJdbcTemplate db=mock(NamedParameterJdbcTemplate.class);
  var subject=new SetupController(db,mock(PasswordEncoder.class),"a".repeat(48));
  var request=new SetupController.Request(
    new SetupController.Company("Example","contact@example.com","Europe/Berlin","de","EUR","",""),
    new SetupController.Admin("admin@example.com","correct horse battery staple"),
    List.of("website"),"light","dark");
  assertThrows(org.springframework.web.server.ResponseStatusException.class,
    ()->subject.complete("bad-token",request));
  verifyNoInteractions(db);
 }
 @Test void missingConfiguredTokenAlwaysRejects(){
  var subject=new SetupController(mock(NamedParameterJdbcTemplate.class),mock(PasswordEncoder.class),"");
  assertThrows(org.springframework.web.server.ResponseStatusException.class,
    ()->subject.complete("a".repeat(48),null));
 }
}
