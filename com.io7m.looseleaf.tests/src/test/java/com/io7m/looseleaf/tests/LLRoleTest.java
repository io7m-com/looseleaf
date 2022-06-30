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

import com.io7m.looseleaf.security.LLAction;
import com.io7m.looseleaf.security.LLRole;
import com.io7m.looseleaf.security.LLRoleName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

public final class LLRoleTest
{
  /**
   * Regardless of the key name used, an empty list of grants results in
   * rejected write permissions.
   */

  @TestFactory
  public Stream<DynamicTest> testEmptyDenyWrite()
  {
    final var emptyRole =
      new LLRole(new LLRoleName("x"), List.of());

    return LLGenerators.keyNames()
      .limit(100L)
      .map(keyName -> {
        return DynamicTest.dynamicTest("testEmptyDeny_" + keyName, () -> {
          assertFalse(emptyRole.allows(LLAction.WRITE, keyName));
        });
      });
  }

  /**
   * Regardless of the key name used, an empty list of grants results in
   * rejected read permissions.
   */

  @TestFactory
  public Stream<DynamicTest> testEmptyDenyRead()
  {
    final var emptyRole =
      new LLRole(new LLRoleName("x"), List.of());

    return LLGenerators.keyNames()
      .limit(100L)
      .map(keyName -> {
        return DynamicTest.dynamicTest("testEmptyDeny_" + keyName, () -> {
          assertFalse(emptyRole.allows(LLAction.READ, keyName));
        });
      });
  }
}
