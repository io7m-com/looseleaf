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
import com.io7m.looseleaf.security.LLUser;
import com.io7m.looseleaf.security.LLUserName;
import com.io7m.looseleaf.server.internal.telemetry.LLTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceType;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.api.logs.Severity.ERROR;
import static io.opentelemetry.api.logs.Severity.INFO;

/**
 * A metrics service.
 */

public final class LLMetricsService implements RPServiceType
{
  private static final AttributeKey<String> USER =
    stringKey("looseleaf.user");
  private static final AttributeKey<String> KEY =
    stringKey("looseleaf.key");
  private static final AttributeKey<String> OPERATION =
    stringKey("looseleaf.operation");
  private static final AttributeKey<String> REMOTE_ADDRESS =
    stringKey("looseleaf.remoteAddress");

  private final LongCounter reads;
  private final ObservableLongGauge dbTimeGauge;
  private final LongCounter errors;
  private final Logger logger;
  private final LongCounter authErrors;
  private final LLDatabaseType database;
  private final ObservableLongGauge dbSizeGauge;
  private final ObservableLongGauge dbKeysGauge;
  private final ObservableLongGauge httpTimeGauge;
  private final LongCounter httpError400;
  private final LongCounter httpError500;
  private final LongCounter httpCount;
  private final ObservableLongGauge up;
  private volatile long readsV;
  private final LongCounter writes;
  private volatile long writesV;
  private final LongCounter deletes;
  private volatile long deletesV;
  private volatile Duration dbTime;
  private volatile long errorsV;
  private volatile long httpTime;
  private volatile long http400;
  private volatile long http500;

  /**
   * A metrics service.
   *
   * @param inDatabase The database service
   * @param telemetry  The telemetry service
   */

  public LLMetricsService(
    final LLDatabaseService inDatabase,
    final LLTelemetryServiceType telemetry)
  {
    this.database =
      inDatabase.database();

    this.logger =
      telemetry.logger();

    this.dbTime =
      Duration.ZERO;

    this.reads =
      telemetry.meter()
        .counterBuilder("looseleaf_db_reads")
        .setDescription("The number of database reads performed.")
        .build();

    this.writes =
      telemetry.meter()
        .counterBuilder("looseleaf_db_writes")
        .setDescription("The number of database writes performed.")
        .build();

    this.errors =
      telemetry.meter()
        .counterBuilder("looseleaf_db_errors")
        .setDescription("The number of database errors.")
        .build();

    this.authErrors =
      telemetry.meter()
        .counterBuilder("looseleaf_auth_errors")
        .setDescription("The number of failed authentication attempts.")
        .build();

    this.deletes =
      telemetry.meter()
        .counterBuilder("looseleaf_db_deletes")
        .setDescription("The number of database deletes performed.")
        .build();

    this.dbTimeGauge =
      telemetry.meter()
        .gaugeBuilder("looseleaf_db_time")
        .setDescription(
          "The length of time the most recent database operation took (nanoseconds).")
        .ofLongs()
        .buildWithCallback(m -> m.record(this.dbTime.toNanos()));

    this.dbKeysGauge =
      telemetry.meter()
        .gaugeBuilder("looseleaf_db_keys")
        .setDescription("The approximate number of keys in the database.")
        .ofLongs()
        .buildWithCallback(m -> {
          try {
            m.record(this.database.keyCountApproximate());
          } catch (final IOException e) {
            // Nothing we can do about this.
          }
        });

    this.dbSizeGauge =
      telemetry.meter()
        .gaugeBuilder("looseleaf_db_size")
        .setDescription("The approximate size of the database.")
        .ofLongs()
        .buildWithCallback(m -> {
          try {
            m.record(this.database.dataSizeApproximate());
          } catch (final IOException e) {
            // Nothing we can do about this.
          }
        });

    this.httpTimeGauge =
      telemetry.meter()
        .gaugeBuilder("looseleaf_http_request_time")
        .setDescription(
          "The length of time requests are taking to process (nanoseconds).")
        .ofLongs()
        .buildWithCallback(m -> m.record(this.httpTime));

    this.httpError400 =
      telemetry.meter()
        .counterBuilder("looseleaf_http_error_400")
        .setDescription("The number of 4** errors.")
        .build();

    this.httpError500 =
      telemetry.meter()
        .counterBuilder("looseleaf_http_error_500")
        .setDescription("The number of 5** errors.")
        .build();

    this.httpCount =
      telemetry.meter()
        .counterBuilder("looseleaf_http_request")
        .setDescription("The number of requests.")
        .build();

    this.up =
      telemetry.meter()
        .gaugeBuilder("looseleaf_up")
        .setDescription(
          "The looseleaf server is up.")
        .ofLongs()
        .buildWithCallback(m -> m.record(1L));
  }

  @Override
  public String description()
  {
    return "Metrics service.";
  }

  /**
   * @return The number of reads so far
   */

  public long reads()
  {
    return this.readsV;
  }

  /**
   * @return The number of writes so far
   */

  public long writes()
  {
    return this.writesV;
  }

  /**
   * @return The number of deletes so far
   */

  public long deletes()
  {
    return this.deletesV;
  }

  @Override
  public String toString()
  {
    return "[%s 0x%s]".formatted(
      this.getClass().getSimpleName(),
      Long.toUnsignedString(this.hashCode(), 16)
    );
  }

  /**
   * Set the most recent DB time.
   *
   * @param time The time
   */

  public void addDBTime(
    final Duration time)
  {
    this.dbTime = Objects.requireNonNull(time, "time");
  }

  /**
   * Log an error.
   *
   * @param user    The user
   * @param message The error message
   */

  public void logError(
    final LLUser user,
    final String message)
  {
    this.logger.logRecordBuilder()
      .setBody(message)
      .setAttribute(USER, user.name().name())
      .setSeverity(ERROR)
      .emit();

    this.errors.add(1L);
    this.errorsV += 1L;
  }

  /**
   * Log a read operation.
   *
   * @param user The user
   * @param key  The database key
   */

  public void logRead(
    final LLUser user,
    final String key)
  {
    this.logger.logRecordBuilder()
      .setBody("read " + key)
      .setAttribute(OPERATION, "read")
      .setAttribute(KEY, key)
      .setAttribute(USER, user.name().name())
      .setSeverity(INFO)
      .emit();

    this.reads.add(1L);
    this.readsV += 1L;
  }

  /**
   * Log a write operation.
   *
   * @param user The user
   * @param key  The  database key
   */

  public void logUpdate(
    final LLUser user,
    final String key)
  {
    this.logger.logRecordBuilder()
      .setBody("update " + key)
      .setAttribute(OPERATION, "update")
      .setAttribute(KEY, key)
      .setAttribute(USER, user.name().name())
      .setSeverity(INFO)
      .emit();

    this.writes.add(1L);
    this.writesV += 1L;
  }

  /**
   * Log a delete operation.
   *
   * @param user The user
   * @param key  The  database key
   */

  public void logDelete(
    final LLUser user,
    final String key)
  {
    this.logger.logRecordBuilder()
      .setBody("delete " + key)
      .setAttribute(OPERATION, "delete")
      .setAttribute(KEY, key)
      .setAttribute(USER, user.name().name())
      .setSeverity(INFO)
      .emit();

    this.deletes.add(1L);
    this.deletesV += 1L;
  }

  /**
   * Log an authentication error.
   *
   * @param address The remote host
   */

  public void logAuthError(
    final String address)
  {
    this.logger.logRecordBuilder()
      .setBody("Authentication failed.")
      .setAttribute(OPERATION, "authenticate")
      .setAttribute(REMOTE_ADDRESS, address)
      .setSeverity(ERROR)
      .emit();

    this.authErrors.add(1L);
  }

  /**
   * Log an authentication error.
   *
   * @param userName The user name
   * @param address  The remote host
   */

  public void logAuthErrorNamed(
    final LLUserName userName,
    final String address)
  {
    this.logger.logRecordBuilder()
      .setBody("Authentication failed.")
      .setAttribute(OPERATION, "authenticate")
      .setAttribute(USER, userName.name())
      .setAttribute(REMOTE_ADDRESS, address)
      .setSeverity(ERROR)
      .emit();

    this.authErrors.add(1L);
  }

  /**
   * Log a request time.
   *
   * @param time The time
   */

  public void logRequestTime(
    final Duration time)
  {
    this.httpTime = time.toNanos();
  }

  /**
   * Log 4xx response.
   */

  public void logRequest4xx()
  {
    this.httpError400.add(1L);
  }

  /**
   * Log 5xx response.
   */

  public void logRequest5xx()
  {
    this.httpError500.add(1L);
  }

  /**
   * Log a request.
   */

  public void logRequest()
  {
    this.httpCount.add(1L);
  }
}
