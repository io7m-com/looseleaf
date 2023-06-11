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
import com.io7m.repetoir.core.RPServiceDirectoryType;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * A versioning servlet.
 */

public final class LLVersions extends HttpServlet
{
  private final ObjectMapper mapper;

  /**
   * A versioning servlet.
   *
   * @param inServices The service directory
   */

  public LLVersions(
    final RPServiceDirectoryType inServices)
  {
    this.mapper = new ObjectMapper();
  }

  @Override
  protected void service(
    final HttpServletRequest request,
    final HttpServletResponse response)
    throws IOException
  {
    response.setContentType("application/json");
    response.setStatus(200);

    final var array = this.mapper.createArrayNode();
    final var v1 = this.mapper.createObjectNode();
    v1.put("name", "com.io7m.looseleaf.v1");
    v1.put("base", "/v1");
    array.add(v1);

    final var data = this.mapper.writeValueAsBytes(array);
    response.setContentLength(data.length + 2);

    try (var output = response.getOutputStream()) {
      output.write(data);
      output.write('\r');
      output.write('\n');
    }
  }
}
