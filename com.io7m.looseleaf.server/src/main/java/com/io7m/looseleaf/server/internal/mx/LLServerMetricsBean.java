/*
 * Copyright © 2021 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.looseleaf.server.internal.mx;

/**
 * The metrics bean implementation.
 */

public final class LLServerMetricsBean implements LLServerMetricsMXBean
{
  private volatile long writes;
  private volatile long reads;
  private volatile long deletes;

  /**
   * The metrics bean implementation.
   */

  public LLServerMetricsBean()
  {

  }

  @Override
  public long getWrites()
  {
    return this.writes;
  }

  @Override
  public long getReads()
  {
    return this.reads;
  }

  @Override
  public long getDeletes()
  {
    return this.deletes;
  }

  @Override
  public void addWrites(
    final long count)
  {
    this.writes += count;
  }

  @Override
  public void addReads(
    final long count)
  {
    this.reads += count;
  }

  @Override
  public void addDeletes(
    final long count)
  {
    this.deletes += count;
  }
}
