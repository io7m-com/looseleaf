/*
 * Copyright © 2022 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.looseleaf.security;

import com.io7m.jdeferthrow.core.ExceptionTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The context required to make security decisions.
 */

public final class LLSecurityContext
{
  private final Map<LLRoleName, LLRole> roles;
  private final Map<LLUserName, LLUser> users;

  private LLSecurityContext(
    final Map<LLRoleName, LLRole> inRoles,
    final Map<LLUserName, LLUser> inUsers)
  {
    this.roles =
      Objects.requireNonNull(inRoles, "roles");
    this.users =
      Objects.requireNonNull(inUsers, "users");
  }

  /**
   * @return A new builder for an empty context
   */

  public static Builder builder()
  {
    return new Builder();
  }

  /**
   * @return The set of roles
   */

  public Map<LLRoleName, LLRole> roles()
  {
    return this.roles;
  }

  /**
   * @return The set of users
   */

  public Map<LLUserName, LLUser> users()
  {
    return this.users;
  }

  /**
   * A mutable security context builder.
   */

  public static final class Builder
  {
    private final HashMap<LLRoleName, LLRole> roles;
    private final HashMap<LLUserName, LLUser> users;

    private Builder()
    {
      this.roles = new HashMap<>();
      this.users = new HashMap<>();
    }

    /**
     * Add a user to the context.
     *
     * @param userName  The name
     * @param password  The password
     * @param roleNames The user's roles
     *
     * @return this
     */

    public Builder addUser(
      final LLUserName userName,
      final LLPassword password,
      final List<LLRoleName> roleNames)
    {
      Objects.requireNonNull(userName, "userName");
      Objects.requireNonNull(password, "password");
      Objects.requireNonNull(roleNames, "roles");

      final var exceptions =
        new ExceptionTracker<IllegalArgumentException>();

      final var userRoles = new HashMap<LLRoleName, LLRole>();
      for (final var roleName : roleNames) {
        if (userRoles.containsKey(roleName)) {
          exceptions.addException(
            new IllegalArgumentException(
              "Duplicate user role '%s'".formatted(roleName.name()))
          );
          continue;
        }

        final var role = this.roles.get(roleName);
        if (role == null) {
          exceptions.addException(
            new IllegalArgumentException(
              "Nonexistent role '%s'".formatted(roleName.name()))
          );
          continue;
        }
        userRoles.put(roleName, role);
      }

      final var user = new LLUser(userName, password, Map.copyOf(userRoles));
      if (this.users.containsKey(userName)) {
        exceptions.addException(
          new IllegalArgumentException(
            "Duplicate user '%s'".formatted(userName.name()))
        );
      }

      exceptions.throwIfNecessary();
      this.users.put(userName, user);
      return this;
    }


    /**
     * Add a role to the context.
     *
     * @param role The role
     *
     * @return this
     */

    public Builder addRole(
      final LLRole role)
    {
      Objects.requireNonNull(role, "role");

      if (this.roles.containsKey(role.name())) {
        throw new IllegalArgumentException(
          "Duplicate role '%s'".formatted(role.name().name())
        );
      }
      this.roles.put(role.name(), role);
      return this;
    }

    /**
     * @return A complete security context
     */

    public LLSecurityContext build()
    {
      return new LLSecurityContext(
        Map.copyOf(this.roles),
        Map.copyOf(this.users)
      );
    }
  }
}
