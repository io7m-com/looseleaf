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

import java.util.Objects;

/**
 * A grant of permission to perform an action on a set of keys.
 *
 * @param action The action
 * @param keys   The key expression
 */

public record LLGrant(
  LLAction action,
  LLKeyExpression keys)
{
  /**
   * A grant of permission to perform an action on a set of keys.
   *
   * @param action The action
   * @param keys   The key expression
   */

  public LLGrant
  {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(keys, "keys");
  }

  /**
   * Determine if this grant applies to the given key name.
   *
   * @param keyName The key name
   *
   * @return {@code true} if this grant applies to the key
   */

  public boolean matches(
    final LLKeyName keyName)
  {
    return this.keys.matches(keyName);
  }
}
