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

import com.io7m.looseleaf.database.api.LLDatabaseRUD;
import com.io7m.looseleaf.database.api.LLDatabaseType;
import com.io7m.looseleaf.protocol.v1.LLv1Error;
import com.io7m.looseleaf.protocol.v1.LLv1Errors;
import com.io7m.looseleaf.security.LLKeyName;
import com.io7m.looseleaf.security.LLUser;
import com.io7m.looseleaf.server.internal.LLDatabaseService;
import com.io7m.looseleaf.server.internal.LLHTTPErrorStatusException;
import com.io7m.looseleaf.server.internal.LLStrings;
import com.io7m.looseleaf.server.internal.LLv1MessagesService;
import com.io7m.looseleaf.server.internal.auth.LLUserPrincipal;
import com.io7m.looseleaf.server.internal.mx.LLMetricsService;
import com.io7m.looseleaf.server.internal.services.LLServices;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.io7m.looseleaf.security.LLAction.WRITE;

/**
 * The v1 "delete" servlet.
 */

public final class LLDeleteServlet extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLDeleteServlet.class);

  private final LLv1MessagesService messages;
  private final LLDatabaseType database;
  private final LLStrings strings;
  private final LLMetricsService metrics;

  /**
   * The v1 "delete" servlet.
   *
   * @param inServices The service directory
   */

  public LLDeleteServlet(
    final LLServices inServices)
  {
    this.messages =
      inServices.requireService(LLv1MessagesService.class);
    this.database =
      inServices.requireService(LLDatabaseService.class).database();
    this.strings =
      inServices.requireService(LLStrings.class);
    this.metrics =
      inServices.requireService(LLMetricsService.class);
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    final var userPrincipal =
      (LLUserPrincipal) request.getUserPrincipal();
    final var user =
      userPrincipal.user();

    try {
      MDC.put("user", user.name().name());
      MDC.put(
        "client",
        request.getRemoteAddr() + ":" + request.getRemotePort());
      this.doProcessMessage(request, response, user);
    } finally {
      MDC.remove("user");
      MDC.remove("client");
    }
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

      if (!user.allows(WRITE, keyName)) {
        throw new LLHTTPErrorStatusException(
          400,
          "operation-not-permitted",
          this.strings.format(
            "errorOperationNotPermitted",
            WRITE,
            keyName.value())
        );
      }

      this.database.readUpdateDelete(
        new LLDatabaseRUD(
          Set.of(),
          Map.of(),
          Set.of(keyName)
        )
      );

      this.updateMetrics();
      LOG.info("delete {}", keyName.value());
      response.setStatus(200);
      response.setContentLength(0);
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

  private void updateMetrics()
  {
    final var bean = this.metrics.bean();
    bean.addWrites(1L);
  }
}
