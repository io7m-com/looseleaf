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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

/**
 * A filter that tracks request times.
 */

public final class LLServerRequestTimeFilter implements Filter
{
  private final LLServerClock clock;
  private final LLMetricsService metrics;

  /**
   * A filter that tracks request times.
   *
   * @param inMetrics The metrics
   * @param inClock   The clock
   */

  public LLServerRequestTimeFilter(
    final LLMetricsService inMetrics,
    final LLServerClock inClock)
  {
    this.metrics =
      Objects.requireNonNull(inMetrics, "inMetrics");
    this.clock =
      Objects.requireNonNull(inClock, "clock");
  }

  @Override
  public void doFilter(
    final ServletRequest request,
    final ServletResponse response,
    final FilterChain chain)
    throws IOException, ServletException
  {
    final var timeThen = this.clock.nowPrecise();
    try {
      chain.doFilter(request, response);
    } finally {
      final var timeNow = this.clock.nowPrecise();
      this.metrics.logRequestTime(Duration.between(timeThen, timeNow));
    }
  }
}
