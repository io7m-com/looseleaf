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

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A transactional read-update-delete operation. First, all keys in {@code read}
 * will be read, then all keys in {@code update} will be updated, and finally
 * all keys in {@code delete} will be deleted.
 *
 * @param read   The keys to read
 * @param update The keys to update
 * @param delete The keys to delete
 */

public record LLDatabaseRUD(
  Set<LLKeyName> read,
  Map<LLKeyName, String> update,
  Set<LLKeyName> delete)
{
  /**
   * A transactional read-update-delete operation. First, all keys in
   * {@code read} will be read, then all keys in {@code update} will be updated,
   * and finally all keys in {@code delete} will be deleted.
   *
   * @param read   The keys to read
   * @param update The keys to update
   * @param delete The keys to delete
   */

  public LLDatabaseRUD
  {
    Objects.requireNonNull(read, "read");
    Objects.requireNonNull(update, "update");
    Objects.requireNonNull(delete, "delete");
  }
}
