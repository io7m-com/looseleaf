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


package com.io7m.looseleaf.tests;

import com.io7m.looseleaf.security.LLPassword;
import com.io7m.looseleaf.security.LLPasswordAlgorithmPBKDF2HmacSHA256;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LLPasswordTest
{
  @Test
  public void testCase()
  {
    assertThrows(IllegalArgumentException.class, () -> {
      new LLPassword(
        LLPasswordAlgorithmPBKDF2HmacSHA256.create(),
        "abcdef0123456789",
        "ABCDEF0123456789"
      );
    });

    assertThrows(IllegalArgumentException.class, () -> {
      new LLPassword(
        LLPasswordAlgorithmPBKDF2HmacSHA256.create(),
        "ABCDEF0123456789",
        "abcdef0123456789"
      );
    });

    final var password =
      new LLPassword(
        LLPasswordAlgorithmPBKDF2HmacSHA256.create(),
        "ABCDEF",
        "ABCDEF0123456789"
      );

    assertEquals("ABCDEF", password.hash());
    assertEquals("ABCDEF0123456789", password.salt());
  }
}
