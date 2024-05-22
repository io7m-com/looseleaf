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

package com.io7m.looseleaf.cmdline.internal;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.io7m.looseleaf.security.LLPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.looseleaf.server.api.LLServerHashedPassword;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;

import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * Create a hashed password.
 */

public final class LLCreatePassword implements QCommandType
{
  private static final QParameterNamed1<String> PASSWORD =
    new QParameterNamed1<>(
      "--password",
      List.of(),
      new QStringType.QConstant("The password."),
      Optional.empty(),
      String.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public LLCreatePassword()
  {
    this.metadata = new QCommandMetadata(
      "create-password",
      new QStringType.QConstant("Create a hashed password."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(List.of(PASSWORD));
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    QLogback.configure(context);

    final var algorithm =
      LLPasswordAlgorithmPBKDF2HmacSHA256.create();
    final var hashedPassword =
      algorithm.createHashed(context.parameterValue(PASSWORD));

    final var configuration =
      new LLServerHashedPassword(
        hashedPassword.algorithm().identifier(),
        hashedPassword.salt(),
        hashedPassword.hash()
      );

    final var mapper =
      JsonMapper.builder()
        .enable(ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(SORT_PROPERTIES_ALPHABETICALLY)
        .enable(INDENT_OUTPUT)
        .build();

    mapper.writeValue(context.output(), configuration);
    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
