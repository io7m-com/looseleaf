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

package com.io7m.looseleaf.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.looseleaf.security.LLPasswordException;
import com.io7m.looseleaf.security.LLRoleName;
import com.io7m.looseleaf.security.LLSecurityContext;
import com.io7m.looseleaf.security.LLUserName;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A server configuration.
 *
 * @param schema         The schema identifier
 * @param addresses      The server bind addresses
 * @param databaseFile   The server's database file
 * @param databaseKind   The server's database kind (such as "SQLITE")
 * @param roles          The set of user roles
 * @param users          The set of users
 * @param telemetry      The telemetry configuration
 * @param faultInjection The fault injection configuration
 */

@JsonDeserialize
@JsonSerialize
public record LLServerConfiguration(
  @JsonProperty(value = "%schema", required = false)
  String schema,
  @JsonProperty(value = "addresses", required = true)
  List<LLServerAddress> addresses,
  @JsonProperty(value = "databaseFile", required = true)
  Path databaseFile,
  @JsonProperty(value = "databaseKind", required = false)
  Optional<String> databaseKind,
  @JsonProperty(value = "roles", required = true)
  List<LLServerRole> roles,
  @JsonProperty(value = "users", required = true)
  List<LLServerUser> users,
  @JsonProperty(value = "telemetry", required = false)
  Optional<LLTelemetryConfiguration> telemetry,
  @JsonProperty(value = "faultInjection", required = false)
  Optional<LLFaultInjection> faultInjection)
{
  /**
   * A server configuration.
   *
   * @param schema         The schema identifier
   * @param addresses      The server bind addresses
   * @param databaseFile   The server's database file
   * @param databaseKind   The server's database kind (such as "SQLITE")
   * @param roles          The set of user roles
   * @param users          The set of users
   * @param telemetry      The telemetry configuration
   * @param faultInjection The fault injection configuration
   */

  public LLServerConfiguration
  {
    Objects.requireNonNull(addresses, "addresses");
    Objects.requireNonNull(databaseFile, "databaseFile");
    Objects.requireNonNull(roles, "roles");
    Objects.requireNonNull(users, "users");
    Objects.requireNonNull(telemetry, "telemetry");
    Objects.requireNonNull(faultInjection, "faultInjection");
  }

  /**
   * @return The configuration as a security context
   */

  public LLSecurityContext toSecurityContext()
  {
    final var builder =
      LLSecurityContext.builder();

    final var exceptions =
      new ExceptionTracker<IllegalArgumentException>();

    for (final var role : this.roles) {
      try {
        builder.addRole(role.asRole());
      } catch (final IllegalArgumentException e) {
        exceptions.addException(e);
      }
    }

    for (final var user : this.users) {
      try {
        builder.addUser(
          new LLUserName(user.name()),
          user.password()
            .asPassword(),
          user.roles()
            .stream()
            .map(LLRoleName::new)
            .toList()
        );
      } catch (final IllegalArgumentException e) {
        exceptions.addException(e);
      } catch (final LLPasswordException e) {
        exceptions.addException(new IllegalArgumentException(e));
      }
    }

    exceptions.throwIfNecessary();
    return builder.build();
  }
}
