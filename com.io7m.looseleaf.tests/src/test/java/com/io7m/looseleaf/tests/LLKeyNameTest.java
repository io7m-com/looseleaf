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

import com.io7m.looseleaf.security.LLKeyName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class LLKeyNameTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLKeyNameTest.class);

  private static DynamicTest valid(
    final String text)
  {
    return DynamicTest.dynamicTest("testValid_" + text, () -> {
      final var k = LLKeyName.create(text);
      LOG.debug("k: {}", k.value());
    });
  }

  private static DynamicTest invalid(
    final String text)
  {
    return DynamicTest.dynamicTest("testInvalid_" + text, () -> {
      assertThrows(IllegalArgumentException.class, () -> {
        LLKeyName.create(text);
      });
    });
  }

  @Test
  public void testEquals()
  {
    final var k = LLKeyName.create("/a");
    assertEquals(k, k);
    assertEquals(
      LLKeyName.create("/a"),
      LLKeyName.create("/a")
    );
    assertEquals(
      LLKeyName.create("/a").hashCode(),
      LLKeyName.create("/a").hashCode()
    );
    assertEquals(
      LLKeyName.create("/a").toString(),
      LLKeyName.create("/a").toString()
    );
    assertNotEquals(
      LLKeyName.create("/a"),
      LLKeyName.create("/b")
    );
    assertNotEquals(
      LLKeyName.create("/a"),
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

    final var base = "/a/b/c/d/e/f";
    final var k = LLKeyName.create(text.toString());
    assertEquals(base, k.value());
  }

  @TestFactory
  public Stream<DynamicTest> testValid()
  {
    return Stream.of(
      "/x",
      "//x"
    ).map(LLKeyNameTest::valid);
  }

  @TestFactory
  public Stream<DynamicTest> testInvalid()
  {
    return Stream.of(
      "",
      "/"
    ).map(LLKeyNameTest::invalid);
  }
}
