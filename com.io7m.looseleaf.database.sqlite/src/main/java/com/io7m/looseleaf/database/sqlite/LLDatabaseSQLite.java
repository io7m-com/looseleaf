/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.looseleaf.database.sqlite;

import com.io7m.anethum.api.ParsingException;
import com.io7m.looseleaf.database.api.LLDatabaseRUD;
import com.io7m.looseleaf.database.api.LLDatabaseType;
import com.io7m.looseleaf.security.LLKeyName;
import com.io7m.trasco.api.TrArguments;
import com.io7m.trasco.api.TrEventExecutingSQL;
import com.io7m.trasco.api.TrEventType;
import com.io7m.trasco.api.TrEventUpgrading;
import com.io7m.trasco.api.TrException;
import com.io7m.trasco.api.TrExecutorConfiguration;
import com.io7m.trasco.api.TrSchemaRevisionSet;
import com.io7m.trasco.vanilla.TrExecutors;
import com.io7m.trasco.vanilla.TrSchemaRevisionSetParsers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteOpenMode;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.io7m.trasco.api.TrExecutorUpgrade.PERFORM_UPGRADES;
import static java.math.BigInteger.valueOf;

/**
 * A database based on SQLite.
 */

public final class LLDatabaseSQLite
  implements LLDatabaseType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLDatabaseSQLite.class);

  private static final String DATABASE_APPLICATION_ID =
    "com.io7m.looseleaf";

  private static final int DATABASE_SQLITE_ID =
    0x4C454146;

  private final SQLiteDataSource dataSource;
  private final AtomicBoolean closed;

  private LLDatabaseSQLite(
    final SQLiteDataSource inDataSource)
  {
    this.dataSource =
      Objects.requireNonNull(inDataSource, "dataSource");
    this.closed =
      new AtomicBoolean(false);
  }

  private static void schemaVersionSet(
    final BigInteger version,
    final Connection connection)
    throws SQLException
  {
    final String statementText;
    if (Objects.equals(version, BigInteger.ZERO)) {
      statementText = "insert into schema_version (version_application_id, version_number) values (?, ?)";
      try (var statement =
             connection.prepareStatement(statementText)) {
        statement.setString(1, DATABASE_APPLICATION_ID);
        statement.setLong(2, version.longValueExact());
        statement.execute();
      }
    } else {
      statementText = "update schema_version set version_number = ?";
      try (var statement =
             connection.prepareStatement(statementText)) {
        statement.setLong(1, version.longValueExact());
        statement.execute();
      }
    }
  }

  private static Optional<BigInteger> schemaVersionGet(
    final Connection connection)
    throws SQLException
  {
    Objects.requireNonNull(connection, "connection");

    try {
      final var statementText =
        "SELECT version_application_id, version_number FROM schema_version";
      LOG.debug("execute: {}", statementText);

      try (var statement = connection.prepareStatement(statementText)) {
        try (var result = statement.executeQuery()) {
          if (!result.next()) {
            throw new SQLException("schema_version table is empty!");
          }
          final var applicationCA =
            result.getString(1);
          final var version =
            result.getLong(2);

          if (!Objects.equals(applicationCA, DATABASE_APPLICATION_ID)) {
            throw new SQLException(
              String.format(
                "Database application ID is %s but should be %s",
                applicationCA,
                DATABASE_APPLICATION_ID
              )
            );
          }

          return Optional.of(valueOf(version));
        }
      }
    } catch (final SQLException e) {
      if (e.getErrorCode() == SQLiteErrorCode.SQLITE_ERROR.code) {
        connection.rollback();
        return Optional.empty();
      }
      throw e;
    }
  }

  /**
   * Open an SQLite database.
   *
   * @param file The database file
   *
   * @return The database
   *
   * @throws IOException On errors
   */

  public static LLDatabaseType open(
    final Path file)
    throws IOException
  {
    try {
      final var absFile = file.toAbsolutePath();
      createOrUpgrade(absFile);
      return doOpen(absFile);
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  private static LLDatabaseType doOpen(
    final Path file)
  {
    final var url = new StringBuilder(128);
    url.append("jdbc:sqlite:");
    url.append(file);

    final var config = new SQLiteConfig();
    config.setApplicationId(DATABASE_SQLITE_ID);
    config.enforceForeignKeys(true);
    config.setLockingMode(SQLiteConfig.LockingMode.NORMAL);
    config.setJournalMode(SQLiteConfig.JournalMode.WAL);

    final var dataSource = new SQLiteDataSource(config);
    dataSource.setUrl(url.toString());
    return new LLDatabaseSQLite(dataSource);
  }

  private static void setWALMode(
    final Connection connection)
    throws SQLException
  {
    try (var st = connection.createStatement()) {
      st.execute("PRAGMA journal_mode=WAL;");
    }
  }

  private static void createOrUpgrade(
    final Path file)
    throws SQLException, TrException, IOException, ParsingException
  {
    final var url = new StringBuilder(128);
    url.append("jdbc:sqlite:");
    url.append(file);

    final var config = new SQLiteConfig();
    config.setApplicationId(DATABASE_SQLITE_ID);
    config.enforceForeignKeys(true);
    config.setOpenMode(SQLiteOpenMode.CREATE);
    config.setLockingMode(SQLiteConfig.LockingMode.NORMAL);
    config.setJournalMode(SQLiteConfig.JournalMode.WAL);

    final var dataSource = new SQLiteDataSource(config);
    dataSource.setUrl(url.toString());

    final var parsers = new TrSchemaRevisionSetParsers();
    final TrSchemaRevisionSet revisions;
    try (var stream = LLDatabaseSQLite.class.getResourceAsStream(
      "/com/io7m/looseleaf/database/sqlite/database.xml")) {
      revisions = parsers.parse(URI.create("urn:source"), stream);
    }

    final var arguments =
      new TrArguments(Map.of());

    try (var connection = dataSource.getConnection()) {
      setWALMode(connection);
      connection.setAutoCommit(false);

      new TrExecutors().create(
        new TrExecutorConfiguration(
          LLDatabaseSQLite::schemaVersionGet,
          LLDatabaseSQLite::schemaVersionSet,
          LLDatabaseSQLite::logEvent,
          revisions,
          PERFORM_UPGRADES,
          arguments,
          connection
        )
      ).execute();
      connection.commit();
    }
  }

  private static void logEvent(
    final TrEventType event)
  {
    switch (event) {
      case final TrEventExecutingSQL sql -> {
        LOG.trace("Executing SQL: {}", sql.statement());
      }
      case final TrEventUpgrading upgrading -> {
        LOG.info(
          "Upgrading schema: {} -> {}",
          upgrading.fromVersion(),
          upgrading.toVersion()
        );
      }
    }
  }

  @Override
  public long dataSizeApproximate()
    throws IOException
  {
    try (var conn = this.connection()) {
      try (var st = conn.prepareStatement(
        "SELECT sum(length(kv.kv_value)) FROM key_value_store AS kv")) {
        try (var rs = st.executeQuery()) {
          while (rs.next()) {
            return rs.getLong(1);
          }
          return 0L;
        }
      }
    } catch (final SQLException e) {
      throw new IOException(e);
    }
  }

  private Connection connection()
    throws SQLException
  {
    final var connection = this.dataSource.getConnection();
    setWALMode(connection);
    connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    connection.setAutoCommit(false);
    return connection;
  }

  @Override
  public long keyCountApproximate()
    throws IOException
  {
    final var query =
      "SELECT count(kv.kv_name) FROM key_value_store AS kv";

    try (var conn = this.connection()) {
      try (var st = conn.prepareStatement(query)) {
        try (var rs = st.executeQuery()) {
          while (rs.next()) {
            return rs.getLong(1);
          }
          return 0L;
        }
      }
    } catch (final SQLException e) {
      throw new IOException(e);
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
    try (var conn = this.connection()) {
      final var r = read(conn, rud);
      update(conn, rud);
      delete(conn, rud);
      conn.commit();
      return r;
    } catch (final SQLException e) {
      throw new IOException(e);
    }
  }

  private static void delete(
    final Connection conn,
    final LLDatabaseRUD rud)
    throws SQLException
  {
    final var query = """
      DELETE FROM key_value_store AS kv
      WHERE kv.kv_name = ?
            """;

    try (var st = conn.prepareStatement(query)) {
      for (final var r : rud.delete()) {
        st.setString(1, r.value());
        st.addBatch();
      }
      st.executeBatch();
    }
  }

  private static void update(
    final Connection conn,
    final LLDatabaseRUD rud)
    throws SQLException
  {
    final var query = """
      INSERT INTO key_value_store
        VALUES(?, ?)
        ON CONFLICT(kv_name)
        DO UPDATE SET kv_value = ?;
            """;

    try (var st = conn.prepareStatement(query)) {
      for (final var entry : rud.update().entrySet()) {
        st.setString(1, entry.getKey().value());
        st.setString(2, entry.getValue());
        st.setString(3, entry.getValue());
        st.addBatch();
      }
      st.executeBatch();
    }
  }

  private static Map<LLKeyName, String> read(
    final Connection conn,
    final LLDatabaseRUD rud)
    throws SQLException
  {
    final var query =
      "SELECT kv.kv_value FROM key_value_store AS kv WHERE kv.kv_name = ?";

    final var r = new HashMap<LLKeyName, String>();
    try (var st = conn.prepareStatement(query)) {
      for (final var rr : rud.read()) {
        st.setString(1, rr.value());
        try (var rs = st.executeQuery()) {
          if (rs.next()) {
            r.put(rr, rs.getString(1));
          }
        }
      }
    }
    return Map.copyOf(r);
  }

  @Override
  public Optional<String> get(
    final LLKeyName key)
    throws IOException
  {
    final var query =
      "SELECT kv.kv_value FROM key_value_store AS kv WHERE kv.kv_name = ?";

    try (var conn = this.connection()) {
      try (var st = conn.prepareStatement(query)) {
        st.setString(1, key.value());

        try (var rs = st.executeQuery()) {
          while (rs.next()) {
            return Optional.of(rs.getString(1));
          }
          return Optional.empty();
        }
      }
    } catch (final SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Map<LLKeyName, String> getAll()
    throws IOException
  {
    final var query =
      "SELECT kv.kv_name, kv.kv_value FROM key_value_store AS kv";

    final HashMap<LLKeyName, String> r = new HashMap<>();
    try (var conn = this.connection()) {
      try (var st = conn.prepareStatement(query)) {
        try (var rs = st.executeQuery()) {
          while (rs.next()) {
            r.put(
              LLKeyName.create(rs.getString(1)),
              rs.getString(2)
            );
          }
        }
      }
    } catch (final SQLException e) {
      throw new IOException(e);
    }
    return Map.copyOf(r);
  }

  @Override
  public void close()
  {
    if (this.closed.compareAndSet(false, true)) {
      // Nothing to close, currently
    }
  }
}
