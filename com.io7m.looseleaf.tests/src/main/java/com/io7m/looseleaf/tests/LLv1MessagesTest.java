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

import com.io7m.looseleaf.protocol.v1.LLv1Error;
import com.io7m.looseleaf.protocol.v1.LLv1Errors;
import com.io7m.looseleaf.protocol.v1.LLv1Messages;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class LLv1MessagesTest
{
  private LLv1Messages messages;

  @BeforeEach
  public void setup()
  {
    this.messages = new LLv1Messages();
  }

  @AfterEach
  public void tearDown()
  {

  }

  @Test
  public void testError()
    throws IOException
  {
    final var m0 =
      new LLv1Error("errorCode", "message");
    final var b =
      this.messages.serialize(m0);
    final var m1 =
      this.messages.deserialize(b);

    assertEquals(m0, m1);
  }

  @Test
  public void testErrors()
    throws IOException
  {
    final var m0 =
      new LLv1Errors(List.of(
        new LLv1Error("errorCode", "message"),
        new LLv1Error("errorCode", "message"),
        new LLv1Error("errorCode", "message")
      ));

    final var b =
      this.messages.serialize(m0);
    final var m1 =
      this.messages.deserialize(b);

    assertEquals(m0, m1);
  }
}
