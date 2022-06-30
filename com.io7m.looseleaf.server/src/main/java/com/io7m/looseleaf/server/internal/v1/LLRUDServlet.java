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
import com.io7m.looseleaf.protocol.v1.LLv1MessageType;
import com.io7m.looseleaf.protocol.v1.LLv1Messages;
import com.io7m.looseleaf.protocol.v1.LLv1RUD;
import com.io7m.looseleaf.protocol.v1.LLv1Result;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.io7m.looseleaf.security.LLAction.READ;
import static com.io7m.looseleaf.security.LLAction.WRITE;

/**
 * The v1 "RUD" servlet.
 */

public final class LLRUDServlet extends HttpServlet
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLRUDServlet.class);

  private final LLv1MessagesService messages;
  private final LLDatabaseType database;
  private final LLStrings strings;
  private final LLMetricsService metrics;

  /**
   * The v1 "RUD" servlet.
   *
   * @param inServices The service directory
   */

  public LLRUDServlet(
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

  private static void sendV1Errors(
    final LLv1Errors errors,
    final HttpServletResponse response,
    final int sc,
    final LLv1Messages v1Messages)
    throws IOException
  {
    final var v1errors = errors;
    response.setContentType("application/json");
    response.setStatus(sc);

    final var data = v1Messages.serialize(v1errors);
    response.setContentLength(data.length + 2);
    try (var output = response.getOutputStream()) {
      output.write(data);
      output.write('\r');
      output.write('\n');
    }
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
    final var v1Messages =
      this.messages.messages();

    try {
      final LLv1RUD message =
        this.readMessage(request);

      final var errors =
        new ArrayList<LLv1Error>();

      final var rud =
        this.checkKeysPermitted(user, message, errors);

      if (!errors.isEmpty()) {
        sendV1Errors(new LLv1Errors(errors), response, 400, v1Messages);
        return;
      }

      final var dbResult =
        this.database.readUpdateDelete(rud);

      for (final var read : rud.read()) {
        LOG.info("read {}", read.value());
      }
      for (final var update : rud.update().keySet()) {
        LOG.info("update {}", update.value());
      }
      for (final var delete : rud.delete()) {
        LOG.info("delete {}", delete.value());
      }

      this.updateMetrics(rud);

      final var result =
        new LLv1Result(
          dbResult.entrySet()
            .stream()
            .map(e -> Map.entry(e.getKey().value(), e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );

      response.setContentType("application/json");
      response.setStatus(200);

      final var data = v1Messages.serialize(result);
      response.setContentLength(data.length + 2);

      try (var output = response.getOutputStream()) {
        output.write(data);
        output.write('\r');
        output.write('\n');
      }
    } catch (final LLHTTPErrorStatusException e) {
      final var errors =
        List.of(new LLv1Error(e.errorCode(), e.getMessage()));
      sendV1Errors(
        new LLv1Errors(errors), response, e.statusCode(), v1Messages);
    }
  }

  private void updateMetrics(
    final LLDatabaseRUD rud)
  {
    final var bean = this.metrics.bean();
    bean.addReads(rud.read().size());
    bean.addWrites(rud.update().size());
    bean.addDeletes(rud.delete().size());
  }

  private LLDatabaseRUD checkKeysPermitted(
    final LLUser user,
    final LLv1RUD message,
    final List<LLv1Error> errors)
  {
    final var keysRead = new HashSet<LLKeyName>();
    for (final var value : message.read()) {
      try {
        final var keyName = LLKeyName.create(value);
        if (!user.allows(READ, keyName)) {
          errors.add(new LLv1Error(
            "operation-not-permitted",
            this.strings.format(
              "errorOperationNotPermitted",
              READ,
              keyName.value())
          ));
        } else {
          keysRead.add(keyName);
        }
      } catch (final IllegalArgumentException e) {
        errors.add(new LLv1Error(
          "bad-key-name",
          this.strings.format("errorBadKeyName", value, e.getMessage()))
        );
      }
    }

    final var keysUpdate = new HashMap<LLKeyName, String>();
    for (final var entry : message.update().entrySet()) {
      final var k = entry.getKey();
      final var v = entry.getValue();
      try {
        final var keyName = LLKeyName.create(k);
        if (!user.allows(READ, keyName)) {
          errors.add(new LLv1Error(
            "operation-not-permitted",
            this.strings.format(
              "errorOperationNotPermitted",
              WRITE,
              keyName.value())
          ));
        } else {
          keysUpdate.put(keyName, v);
        }
      } catch (final IllegalArgumentException e) {
        errors.add(new LLv1Error(
          "bad-key-name",
          this.strings.format("errorBadKeyName", k, e.getMessage()))
        );
      }
    }

    final var keysDelete = new HashSet<LLKeyName>();
    for (final var value : message.delete()) {
      try {
        final var keyName = LLKeyName.create(value);
        if (!user.allows(WRITE, keyName)) {
          errors.add(new LLv1Error(
            "operation-not-permitted",
            this.strings.format(
              "errorOperationNotPermitted",
              WRITE,
              keyName.value())
          ));
        } else {
          keysDelete.add(keyName);
        }
      } catch (final IllegalArgumentException e) {
        errors.add(new LLv1Error(
          "bad-key-name",
          this.strings.format("errorBadKeyName", value, e.getMessage()))
        );
      }
    }

    return new LLDatabaseRUD(keysRead, keysUpdate, keysDelete);
  }

  private LLv1RUD readMessage(
    final HttpServletRequest request)
    throws LLHTTPErrorStatusException
  {
    try {
      final var v1Messages = this.messages.messages();
      try (var stream = request.getInputStream()) {
        final LLv1MessageType message = v1Messages.deserialize(stream);
        if (message instanceof LLv1RUD rud) {
          return rud;
        }
      }
    } catch (final IOException e) {
      throw new LLHTTPErrorStatusException(
        400,
        "bad-message",
        this.strings.format("errorUnparseableMessage", e.getMessage())
      );
    }

    throw new LLHTTPErrorStatusException(
      400,
      "unexpected-message",
      this.strings.format(
        "errorUnexpectedMessage",
        LLv1RUD.class.getSimpleName())
    );
  }
}
