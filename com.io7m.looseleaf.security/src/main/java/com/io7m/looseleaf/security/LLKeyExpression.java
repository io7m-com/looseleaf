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
import java.util.regex.Pattern;

/**
 * A key expression that defines a set of keys.
 */

public final class LLKeyExpression
{
  private static final Pattern SLASHES =
    Pattern.compile("/+");

  private final String value;
  private final boolean wildcard;

  private LLKeyExpression(
    final String inValue)
  {
    Objects.requireNonNull(inValue, "value");

    final var normalized =
      SLASHES.matcher(inValue).replaceAll("/");

    if (!normalized.startsWith("/")) {
      throw new IllegalArgumentException(
        "Key expression '%s' must start with '/'".formatted(normalized));
    }

    if ("/".equals(normalized)) {
      throw new IllegalArgumentException(
        "Key expressions must not equal /");
    }

    this.wildcard = normalized.contains("*");
    if (this.wildcard && !normalized.endsWith("*")) {
      throw new IllegalArgumentException(
        "Wildcards can only appear at the end of key expressions (received '%s')"
          .formatted(normalized)
      );
    }

    this.value = normalized.replace("*", "");
  }

  /**
   * Create a key expression.
   *
   * @param expression The expression
   *
   * @return A key expression
   */

  public static LLKeyExpression create(
    final String expression)
  {
    return new LLKeyExpression(expression);
  }

  @Override
  public boolean equals(final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || !this.getClass().equals(o.getClass())) {
      return false;
    }
    final LLKeyExpression that = (LLKeyExpression) o;
    return this.value.equals(that.value);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.value);
  }

  /**
   * @return The raw expression as a string
   */

  public String value()
  {
    return this.value;
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  /**
   * Determine if this expression includes the given key.
   *
   * @param keyName The key name
   *
   * @return {@code true} if this expression includes the given key
   */

  public boolean matches(
    final LLKeyName keyName)
  {
    if (this.wildcard) {
      return keyName.value().startsWith(this.value);
    }
    return keyName.value().equals(this.value);
  }
}
