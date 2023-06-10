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


package com.io7m.looseleaf.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

/**
 * Fault injection configuration.
 *
 * @param databaseCrashProbability The probability that a database access will crash
 */

@JsonDeserialize
@JsonSerialize
public record LLFaultInjection(
  @JsonProperty(value = "databaseCrashProbability", required = true)
  double databaseCrashProbability)
{
  /**
   * @return The configuration with no fault injection
   */

  public static LLFaultInjection disabled()
  {
    return new LLFaultInjection(0.0);
  }

  /**
   * Potentially throw an I/O exception based on the database crash probability.
   *
   * @throws IOException Sometimes
   */

  public void databaseFaultInject()
    throws IOException
  {
    if (Math.random() < this.databaseCrashProbability()) {
      throw new IOException("Injected database exception.");
    }
  }
}
