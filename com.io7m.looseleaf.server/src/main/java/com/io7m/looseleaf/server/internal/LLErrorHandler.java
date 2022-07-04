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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Dispatcher;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

/**
 * A simple JSON error handler.
 */

public final class LLErrorHandler extends ErrorHandler
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLErrorHandler.class);

  private final ObjectMapper mapper;

  /**
   * A simple JSON error handler.
   */

  public LLErrorHandler()
  {
    this.mapper = new ObjectMapper();
  }

  @Override
  public void handle(
    final String target,
    final Request baseRequest,
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    final var exception =
      (Throwable) baseRequest.getAttribute(Dispatcher.ERROR_EXCEPTION);
    final var message =
      (String) baseRequest.getAttribute(Dispatcher.ERROR_MESSAGE);
    final var errorCode =
      baseRequest.getAttribute(Dispatcher.ERROR_STATUS_CODE);

    if (exception != null) {
      this.exceptionResponse(response, exception);
      return;
    }

    final var obj = this.mapper.createObjectNode();
    obj.put("message", Optional.ofNullable(message).orElse(""));
    obj.put("errorCode", errorCode.toString());

    try (var out = response.getOutputStream()) {
      out.write(this.mapper.writeValueAsBytes(obj));
      out.write('\r');
      out.write('\n');
    }
  }

  private void exceptionResponse(
    final HttpServletResponse response,
    final Throwable exception)
    throws IOException
  {
    LOG.error("exception: ", exception);

    final var obj =
      this.mapper.createObjectNode();

    final var arr =
      this.mapper.createArrayNode();

    for (final var e : exception.getStackTrace()) {
      arr.add("  at %s.%s (%s:%d)".formatted(
        e.getClassName(),
        e.getMethodName(),
        e.getFileName(),
        Integer.valueOf(e.getLineNumber()))
      );
    }

    obj.put("exception", exception.getClass().getCanonicalName());
    obj.put("errorCode", "unexpected-exception");
    obj.put("message", Optional.ofNullable(exception.getMessage()).orElse(""));
    obj.set("trace", arr);

    try (var out = response.getOutputStream()) {
      out.write(this.mapper.writeValueAsBytes(obj));
      out.write('\r');
      out.write('\n');
    }
  }
}
