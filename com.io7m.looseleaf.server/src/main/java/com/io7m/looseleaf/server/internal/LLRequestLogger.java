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


package com.io7m.looseleaf.server.internal;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;

import java.util.Objects;

/**
 * A metrics request logger.
 */

public final class LLRequestLogger implements RequestLog
{
  private final LLMetricsService metrics;

  /**
   * A metrics request logger.
   *
   * @param inMetrics The metrics
   */

  public LLRequestLogger(
    final LLMetricsService inMetrics)
  {
    this.metrics =
      Objects.requireNonNull(inMetrics, "inMetrics");
  }

  @Override
  public void log(
    final Request request,
    final Response response)
  {
    this.metrics.logRequest();

    if (response.getStatus() >= 400) {
      if (response.getStatus() >= 500) {
        this.metrics.logRequest5xx();
      } else {
        this.metrics.logRequest4xx();
      }
    }
  }
}
