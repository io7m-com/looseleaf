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


package com.io7m.looseleaf.server.api;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.io7m.dixmont.core.DmJsonRestrictedDeserializers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * Functions to serialize/deserialize server configurations.
 */

public final class LLServerConfigurations
{
  private final SimpleDeserializers serializers;
  private final JsonMapper mapper;

  /**
   * Functions to serialize/deserialize server configurations.
   */

  public LLServerConfigurations()
  {
    this.serializers =
      DmJsonRestrictedDeserializers.builder()
        .allowClass(LLServerAction.class)
        .allowClass(LLServerAddress.class)
        .allowClass(LLServerConfiguration.class)
        .allowClass(LLServerGrant.class)
        .allowClass(LLServerHashedPassword.class)
        .allowClass(LLServerRole.class)
        .allowClass(LLServerUser.class)
        .allowClass(Path.class)
        .allowClass(String.class)
        .allowClass(int.class)
        .allowClassName(
          "java.util.List<java.lang.String>")
        .allowClassName(
          "java.util.List<com.io7m.looseleaf.server.api.LLServerAddress>")
        .allowClassName(
          "java.util.List<com.io7m.looseleaf.server.api.LLServerUser>")
        .allowClassName(
          "java.util.List<com.io7m.looseleaf.server.api.LLServerRole>")
        .allowClassName(
          "java.util.List<com.io7m.looseleaf.server.api.LLServerGrant>")
        .build();

    this.mapper =
      JsonMapper.builder()
        .enable(USE_BIG_INTEGER_FOR_INTS)
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .build();

    final var simpleModule = new SimpleModule();
    simpleModule.setDeserializers(this.serializers);
    this.mapper.registerModule(simpleModule);
  }

  /**
   * Parse a server configuration.
   *
   * @param stream An input stream
   *
   * @return A configuration
   *
   * @throws IOException On I/O errors
   */

  public LLServerConfiguration parse(
    final InputStream stream)
    throws IOException
  {
    return this.mapper.readValue(stream, LLServerConfiguration.class);
  }

  /**
   * Parse a server configuration.
   *
   * @param file An input file
   *
   * @return A configuration
   *
   * @throws IOException On I/O errors
   */

  public LLServerConfiguration parse(
    final Path file)
    throws IOException
  {
    try (var stream = Files.newInputStream(file)) {
      return this.parse(stream);
    }
  }
}
