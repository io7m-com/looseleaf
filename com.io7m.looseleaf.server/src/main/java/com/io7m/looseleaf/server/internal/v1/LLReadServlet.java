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

package com.io7m.looseleaf.server.internal.v1;

import com.io7m.looseleaf.database.api.LLDatabaseType;
import com.io7m.looseleaf.protocol.v1.LLv1Error;
import com.io7m.looseleaf.protocol.v1.LLv1Errors;
import com.io7m.looseleaf.security.LLKeyName;
import com.io7m.looseleaf.security.LLUser;
import com.io7m.looseleaf.server.api.LLFaultInjection;
import com.io7m.looseleaf.server.api.LLServerConfiguration;
import com.io7m.looseleaf.server.internal.LLConfigurationService;
import com.io7m.looseleaf.server.internal.LLDatabaseService;
import com.io7m.looseleaf.server.internal.LLHTTPErrorStatusException;
import com.io7m.looseleaf.server.internal.LLMetricsService;
import com.io7m.looseleaf.server.internal.LLServerClock;
import com.io7m.looseleaf.server.internal.LLStrings;
import com.io7m.looseleaf.server.internal.LLv1MessagesService;
import com.io7m.looseleaf.server.internal.auth.LLUserPrincipal;
import com.io7m.looseleaf.server.internal.telemetry.LLTelemetryServiceType;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.io7m.looseleaf.security.LLAction.READ;
import static com.io7m.looseleaf.server.internal.v1.LLWithTelemetry.withTelemetry;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The v1 "get" servlet.
 */

public final class LLReadServlet extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLReadServlet.class);

  private final LLv1MessagesService messages;
  private final LLDatabaseType database;
  private final LLStrings strings;
  private final LLMetricsService metrics;
  private final LLTelemetryServiceType telemetry;
  private final LLServerClock clock;
  private final LLServerConfiguration configuration;

  /**
   * The v1 "get" servlet.
   *
   * @param inServices The service directory
   */

  public LLReadServlet(
    final RPServiceDirectoryType inServices)
  {
    this.clock =
      inServices.requireService(LLServerClock.class);
    this.messages =
      inServices.requireService(LLv1MessagesService.class);
    this.database =
      inServices.requireService(LLDatabaseService.class).database();
    this.strings =
      inServices.requireService(LLStrings.class);
    this.metrics =
      inServices.requireService(LLMetricsService.class);
    this.telemetry =
      inServices.requireService(LLTelemetryServiceType.class);
    this.configuration =
      inServices.requireService(LLConfigurationService.class)
        .configuration();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    withTelemetry(
      this.telemetry,
      "Read",
      request,
      () -> {
        final var userPrincipal =
          (LLUserPrincipal) request.getUserPrincipal();
        final var user =
          userPrincipal.user();

        try {
          MDC.put("user", user.name().name());
          MDC.put(
            "client",
            "%s:%d".formatted(request.getRemoteAddr(), request.getRemotePort())
          );
          this.doProcessMessage(request, response, user);
        } finally {
          MDC.remove("user");
          MDC.remove("client");
        }
      }
    );
  }

  private void doProcessMessage(
    final HttpServletRequest request,
    final HttpServletResponse response,
    final LLUser user)
    throws IOException
  {
    final var v1Messages = this.messages.messages();

    try {
      final var keyName =
        LLKeyName.create(request.getPathInfo());

      if (!user.allows(READ, keyName)) {
        throw new LLHTTPErrorStatusException(
          400,
          "operation-not-permitted",
          this.strings.format(
            "errorOperationNotPermitted",
            READ,
            keyName.value())
        );
      }

      final var timeThen =
        this.clock.nowPrecise();

      final Optional<String> value;
      try {
        this.configuration.faultInjection()
          .orElseGet(LLFaultInjection::disabled)
          .databaseFaultInject();

        value = this.database.get(keyName);
      } catch (final Exception e) {
        Span.current().recordException(e);
        this.metrics.logError(user, e.getMessage());
        throw e;
      }

      final var timeNow =
        this.clock.nowPrecise();

      if (value.isEmpty()) {
        throw new LLHTTPErrorStatusException(
          404,
          "not-found",
          this.strings.format(
            "errorNotFound",
            keyName.value())
        );
      }

      final var valueBytes = value.get().getBytes(UTF_8);
      try (var output = response.getOutputStream()) {
        response.setStatus(200);
        response.setContentType("text/plain");
        response.setContentLength(valueBytes.length);
        output.write(valueBytes);
        output.flush();
      }

      this.metrics.logRead(user, keyName.value());
      this.metrics.addDBTime(Duration.between(timeThen, timeNow));

      LOG.info("get {}", keyName.value());
    } catch (final LLHTTPErrorStatusException e) {
      response.setContentType("application/json");
      response.setStatus(e.statusCode());
      try (var output = response.getOutputStream()) {
        output.write(v1Messages.serialize(
          new LLv1Errors(List.of(new LLv1Error(e.errorCode(), e.getMessage()))))
        );
      }
    }
  }
}
