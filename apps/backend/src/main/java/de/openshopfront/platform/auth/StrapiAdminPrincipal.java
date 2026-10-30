package de.openshopfront.platform.auth;

import java.io.Serializable;
import java.security.Principal;

/** Identifies one authenticated Strapi administrator without storing Strapi credentials. */
public record StrapiAdminPrincipal(String id, String name, String email, boolean superAdmin)
    implements Principal, Serializable {
  /** Returns the stable Strapi user ID for Spring Security and session indexes. */
  @Override
  public String getName() {
    return id;
  }
}
