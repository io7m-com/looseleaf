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

package com.io7m.looseleaf.database.api;

import com.io7m.looseleaf.security.LLKeyName;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * A database.
 */

public interface LLDatabaseType extends Closeable
{
  /**
   * @return The approximate size of the database in bytes
   *
   * @throws IOException On errors
   */

  long dataSizeApproximate()
    throws IOException;

  /**
   * @return The approximate number of keys in the database
   *
   * @throws IOException On errors
   */

  long keyCountApproximate()
    throws IOException;

  /**
   * @return {@code true} if the store has been closed
   */

  boolean isClosed();

  /**
   * Atomically read, update, and delete values in the database.
   *
   * @param rud The RUD operation
   *
   * @return The read keys
   *
   * @throws IOException On errors
   */

  Map<LLKeyName, String> readUpdateDelete(LLDatabaseRUD rud)
    throws IOException;

  /**
   * Get the value associated with the given key, if any.
   *
   * @param key The key
   *
   * @return The value, if any
   *
   * @throws IOException On errors
   */

  Optional<String> get(LLKeyName key)
    throws IOException;

  /**
   * @return The entire database
   *
   * @throws IOException On errors
   */

  Map<LLKeyName, String> getAll()
    throws IOException;
}
