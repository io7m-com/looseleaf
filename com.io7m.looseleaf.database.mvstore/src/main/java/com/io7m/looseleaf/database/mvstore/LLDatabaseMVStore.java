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


package com.io7m.looseleaf.database.mvstore;

import com.io7m.looseleaf.database.api.LLDatabaseRUD;
import com.io7m.looseleaf.database.api.LLDatabaseType;
import com.io7m.looseleaf.security.LLKeyName;
import org.h2.engine.IsolationLevel;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.h2.mvstore.tx.TransactionStore;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.h2.mvstore.type.StringDataType.INSTANCE;

/**
 * A database implementation based on the H2 MVStore.
 */

public final class LLDatabaseMVStore
  implements LLDatabaseType
{
  private static final TransactionStore.RollbackListener ROLLBACK_NO_LISTENER =
    (map, key, existingValue, restoredValue) -> {
    };

  private final MVStore store;
  private final TransactionStore txStore;
  private final AtomicBoolean closed;

  /**
   * A database based on the H2 MVStore class.
   *
   * @param inStore   The MV store
   * @param inTXStore The transaction store
   */

  public LLDatabaseMVStore(
    final MVStore inStore,
    final TransactionStore inTXStore)
  {
    this.store =
      Objects.requireNonNull(inStore, "store");
    this.txStore =
      Objects.requireNonNull(inTXStore, "txStore");
    this.closed =
      new AtomicBoolean(false);
  }

  @Override
  public void close()
    throws IOException
  {
    if (this.closed.compareAndSet(false, true)) {
      try {
        this.store.close();
      } catch (final Exception e) {
        throw new IOException(e);
      }
    }
  }

  @Override
  public boolean isClosed()
  {
    return this.closed.get();
  }

  @Override
  public Map<LLKeyName, String> readUpdateDelete(
    final LLDatabaseRUD rud)
    throws IOException
  {
    Objects.requireNonNull(rud, "rud");

    int attempt = 1;
    while (true) {
      try {
        return this.executeRUD(rud);
      } catch (final MVStoreException e) {
        if (attempt == 10) {
          throw e;
        }
        ++attempt;
      }
    }
  }

  private TreeMap<LLKeyName, String> executeRUD(
    final LLDatabaseRUD rud)
  {
    final var tx =
      this.txStore.begin(
        ROLLBACK_NO_LISTENER,
        10,
        0,
        IsolationLevel.READ_COMMITTED
      );

    try {
      final var m =
        tx.openMap("values", INSTANCE, INSTANCE);

      final var results = new TreeMap<LLKeyName, String>();
      for (final var k : rud.read()) {
        final var v = m.get(k.value());
        if (v != null) {
          results.put(k, v);
        }
      }

      for (final var e : rud.update().entrySet()) {
        final var k = e.getKey();
        m.put(k.value(), e.getValue());
      }

      for (final var k : rud.delete()) {
        m.remove(k.value());
      }

      tx.commit();
      return results;
    } catch (final Exception e) {
      tx.rollback();
      throw e;
    }
  }

  @Override
  public Optional<String> get(
    final LLKeyName key)
  {
    Objects.requireNonNull(key, "key");

    final var tx = this.txStore.begin();

    try {
      final var m =
        tx.openMap("values", INSTANCE, INSTANCE);
      return Optional.ofNullable(m.get(key.value()));
    } finally {
      tx.rollback();
    }
  }
}
