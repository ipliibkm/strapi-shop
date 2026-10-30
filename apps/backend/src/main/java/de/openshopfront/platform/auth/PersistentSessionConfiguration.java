package de.openshopfront.platform.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.PostgreSqlJdbcIndexedSessionRepositoryCustomizer;

/** Configures PostgreSQL-specific persistence for administrator sessions. */
@Configuration
public class PersistentSessionConfiguration {
  /** Uses an upsert when concurrent requests create the same session attribute. */
  @Bean
  PostgreSqlJdbcIndexedSessionRepositoryCustomizer postgreSqlSessionRepositoryCustomizer() {
    return new PostgreSqlJdbcIndexedSessionRepositoryCustomizer();
  }
}
