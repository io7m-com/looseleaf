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

import com.io7m.looseleaf.database.api.LLDatabaseType;
import com.io7m.repetoir.core.RPServiceType;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * A service that exposes a database.
 */

public final class LLDatabaseService implements RPServiceType, Closeable
{
  private final LLDatabaseType database;

  /**
   * A service that exposes a database.
   *
   * @param inDatabase The database
   */

  public LLDatabaseService(
    final LLDatabaseType inDatabase)
  {
    this.database = Objects.requireNonNull(inDatabase, "database");
  }

  @Override
  public String description()
  {
    return "Database service.";
  }

  /**
   * @return The database
   */

  public LLDatabaseType database()
  {
    return this.database;
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Long.toUnsignedString(this.hashCode(), 16)
    );
  }

  @Override
  public void close()
    throws IOException
  {
    this.database.close();
  }
}
