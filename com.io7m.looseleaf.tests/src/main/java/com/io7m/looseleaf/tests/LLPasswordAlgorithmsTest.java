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

import com.io7m.looseleaf.security.LLPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.looseleaf.security.LLPasswordAlgorithms;
import com.io7m.looseleaf.security.LLPasswordException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LLPasswordAlgorithmsTest
{
  @Test
  public void testPBKDF2()
    throws Exception
  {
    assertInstanceOf(
      LLPasswordAlgorithmPBKDF2HmacSHA256.class,
      LLPasswordAlgorithms.parse("PBKDF2WithHmacSHA256:10000:256")
    );

    assertEquals(
      "PBKDF2WithHmacSHA256:10000:256",
      LLPasswordAlgorithms.parse("PBKDF2WithHmacSHA256:10000:256")
        .identifier()
    );

    assertEquals(
      "PBKDF2WithHmacSHA256:10000:256",
      LLPasswordAlgorithmPBKDF2HmacSHA256.create()
        .identifier()
    );

    assertEquals(
      "PBKDF2WithHmacSHA256:10000:256",
      LLPasswordAlgorithmPBKDF2HmacSHA256.create(10000, 256)
        .identifier()
    );
  }

  @Test
  public void testPasswordCheck()
    throws Exception
  {
    final var algorithm =
      LLPasswordAlgorithms.parse("PBKDF2WithHmacSHA256:10000:256");
    final var password =
      algorithm.createHashed("password");

    assertFalse(password.check("pass2"));
    assertTrue(password.check("password"));
  }

  @TestFactory
  public Stream<DynamicTest> testUnparseable()
  {
    return Stream.of(
      "",
      "PBKDF2WithHmacSHA256",
      "PBKDF2WithHmacSHA256:10000",
      "PBKDF2WithHmacSHA256:10000:x",
      "PBKDF2WithHmacSHA256:y:245"
    ).map(LLPasswordAlgorithmsTest::testUnparseableOf);
  }

  private static DynamicTest testUnparseableOf(
    final String text)
  {
    return DynamicTest.dynamicTest(
      "testUnparseable_" + text,
      () -> {
        assertThrows(LLPasswordException.class, () -> {
          LLPasswordAlgorithms.parse(text);
        });
      }
    );
  }
}
