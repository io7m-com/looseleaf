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


package com.io7m.looseleaf.server.internal;

import com.io7m.jxtrand.vanilla.JXTAbstractStrings;
import com.io7m.looseleaf.server.internal.services.LLServiceType;

import java.io.IOException;
import java.util.Locale;

/**
 * The server string resource service.
 */

public final class LLStrings
  extends JXTAbstractStrings
  implements LLServiceType
{
  /**
   * The server string resource service.
   *
   * @param locale The server locale
   *
   * @throws IOException On I/O errors
   */

  public LLStrings(
    final Locale locale)
    throws IOException
  {
    super(
      locale,
      LLStrings.class,
      "/com/io7m/looseleaf/server/internal/",
      "Strings"
    );
  }

  @Override
  public String description()
  {
    return "String resource service.";
  }
}
