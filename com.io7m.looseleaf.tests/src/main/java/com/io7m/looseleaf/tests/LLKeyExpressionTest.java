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

import com.io7m.looseleaf.security.LLKeyExpression;
import com.io7m.looseleaf.security.LLKeyName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class LLKeyExpressionTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLKeyExpressionTest.class);

  private static DynamicTest valid(
    final String text)
  {
    return DynamicTest.dynamicTest("testValid_" + text, () -> {
      final var k = LLKeyExpression.create(text);
      LOG.debug("k: {}", k.value());
    });
  }

  private static DynamicTest invalid(
    final String text)
  {
    return DynamicTest.dynamicTest("testInvalid_" + text, () -> {
      assertThrows(IllegalArgumentException.class, () -> {
        LLKeyExpression.create(text);
      });
    });
  }

  @Test
  public void testEquals()
  {
    final var k = LLKeyExpression.create("/a");
    assertEquals(k, k);
    assertEquals(
      LLKeyExpression.create("/a"),
      LLKeyExpression.create("/a")
    );
    assertEquals(
      LLKeyExpression.create("/a").hashCode(),
      LLKeyExpression.create("/a").hashCode()
    );
    assertEquals(
      LLKeyExpression.create("/a").toString(),
      LLKeyExpression.create("/a").toString()
    );
    assertNotEquals(
      LLKeyExpression.create("/a"),
      LLKeyExpression.create("/b")
    );
    assertNotEquals(
      LLKeyExpression.create("/a"),
      Integer.SIZE
    );
  }

  @Test
  public void testNormalize()
  {
    final var text = new StringBuilder();

    for (final var c : List.of('a', 'b', 'c', 'd', 'e', 'f')) {
      text.append("/".repeat((int) (Math.random() * 20) + 1));
      text.append(c);
      text.append("/".repeat((int) (Math.random() * 20) + 1));
    }

    final var base = "/a/b/c/d/e/f/";
    final var k = LLKeyExpression.create(text.toString());
    assertEquals(base, k.value());
  }

  @Test
  public void testAppliesDirect()
  {
    final var ke = LLKeyExpression.create("/a/b");
    assertFalse(ke.matches(LLKeyName.create("/a")));
    assertTrue(ke.matches(LLKeyName.create("/a/b")));
    assertTrue(ke.matches(LLKeyName.create("/a/b/")));
  }

  @Test
  public void testAppliesWildcard()
  {
    final var ke = LLKeyExpression.create("/a/b/*");
    assertFalse(ke.matches(LLKeyName.create("/a")));
    assertFalse(ke.matches(LLKeyName.create("/a/b")));
    assertFalse(ke.matches(LLKeyName.create("/a/b/")));
    assertTrue(ke.matches(LLKeyName.create("/a/b/c")));
  }

  @TestFactory
  public Stream<DynamicTest> testValid()
  {
    return Stream.of(
      "/x",
      "//x",
      "/*"
    ).map(LLKeyExpressionTest::valid);
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    return Stream.of(
      "",
      "/",
      "/*/"
    ).map(LLKeyExpressionTest::invalid);
  }
}
