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

import java.util.List;
import java.util.Objects;

/**
 * A role.
 *
 * @param name   The role name
 * @param grants The access the role grants
 */

public record LLRole(
  LLRoleName name,
  List<LLGrant> grants)
{
  /**
   * A role.
   *
   * @param name   The role name
   * @param grants The access the role grants
   */

  public LLRole
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(grants, "grants");
  }

  /**
   * Determine if the given action is allowed on the given key according to this
   * role.
   *
   * @param action  The action
   * @param keyName The key
   *
   * @return {@code true} if the action is allowed
   */

  public boolean allows(
    final LLAction action,
    final LLKeyName keyName)
  {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(keyName, "keyName");

    return this.grants.stream()
      .anyMatch(grant -> grant.action() == action && grant.matches(keyName));
  }
}
