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

import javax.management.MXBean;

/**
 * Server metrics.
 */

// CHECKSTYLE:OFF
@MXBean
public interface LLServerMetricsMXBean
{
  // CHECKSTYLE:ON

  /**
   * @return The number of writes that have been performed.
   */

  long getWrites();

  /**
   * @return The number of reads that have been performed.
   */

  long getReads();

  /**
   * @return The number of deletes that have been performed.
   */

  long getDeletes();

  /**
   * Increment the number of writes performed.
   *
   * @param count The count to add
   */

  void addWrites(long count);

  /**
   * Increment the number of reads performed.
   *
   * @param count The count to add
   */

  void addReads(long count);

  /**
   * Increment the number of deletes performed.
   *
   * @param count The count to add
   */

  void addDeletes(long count);
}
