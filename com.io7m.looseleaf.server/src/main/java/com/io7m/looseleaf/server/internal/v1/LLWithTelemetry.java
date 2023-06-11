/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.looseleaf.server.internal.telemetry.LLTelemetryServiceType;
import io.opentelemetry.api.trace.SpanKind;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.time.Instant;

import static com.io7m.looseleaf.server.internal.LLServerRequestDecoration.requestIdFor;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_CLIENT_IP;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_METHOD;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_URL;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.HTTP_USER_AGENT;

/**
 * Execute a request under telemetry tracing.
 */

public final class LLWithTelemetry
{
  private LLWithTelemetry()
  {

  }

  /**
   * A function executed inside a span.
   */

  public interface WithTelemetryType
  {
    /**
     * Execute the function.
     *
     * @throws IOException On errors
     */

    void execute()
      throws IOException;
  }

  /**
   * Execute a function in a telemetry span.
   *
   * @param telemetry The telemetry service
   * @param rootName  The span name
   * @param request   The request
   * @param function  The function
   *
   * @throws IOException On errors
   */

  public static void withTelemetry(
    final LLTelemetryServiceType telemetry,
    final String rootName,
    final HttpServletRequest request,
    final WithTelemetryType function)
    throws IOException
  {
    final var span =
      telemetry.tracer()
        .spanBuilder(rootName)
        .setStartTimestamp(Instant.now())
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(HTTP_CLIENT_IP, request.getRemoteAddr())
        .setAttribute(HTTP_METHOD, request.getMethod())
        .setAttribute(
          HTTP_REQUEST_CONTENT_LENGTH,
          request.getContentLengthLong()
        )
        .setAttribute(HTTP_USER_AGENT, request.getHeader("User-Agent"))
        .setAttribute(HTTP_URL, request.getRequestURI())
        .setAttribute("http.request_id", requestIdFor(request).toString())
        .startSpan();

    try (var ignored = span.makeCurrent()) {
      function.execute();
    } catch (final Throwable e) {
      span.recordException(e);
      throw e;
    } finally {
      span.end();
    }
  }
}
