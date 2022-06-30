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
 * The name of a key.
 */

public final class LLKeyName implements Comparable<LLKeyName>
{
  private static final Pattern SLASHES =
    Pattern.compile("/+");
  private static final Pattern SLASHES_END =
    Pattern.compile("/+$");

  private final String value;

  private LLKeyName(
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

    this.value = SLASHES_END.matcher(normalized).replaceAll("");
  }

  /**
   * Create a key name.
   *
   * @param name The name
   *
   * @return A key name
   *
   * @throws IllegalArgumentException On invalid key names
   */

  public static LLKeyName create(
    final String name)
    throws IllegalArgumentException
  {
    return new LLKeyName(name);
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
    final LLKeyName that = (LLKeyName) o;
    return this.value.equals(that.value);
  }

  @Override
  public String toString()
  {
    return this.value;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(this.value);
  }

  /**
   * @return The raw name as a string
   */

  public String value()
  {
    return this.value;
  }

  @Override
  public int compareTo(
    final LLKeyName other)
  {
    return this.value.compareTo(other.value);
  }
}
