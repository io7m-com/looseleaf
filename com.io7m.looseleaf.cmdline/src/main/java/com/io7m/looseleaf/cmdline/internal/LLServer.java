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

import com.io7m.looseleaf.server.LLServers;
import com.io7m.looseleaf.server.api.LLServerConfigurations;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Start a server instance.
 */

public final class LLServer implements QCommandType
{
  private final QCommandMetadata metadata;

  private static final QParameterNamed1<Path> FILE =
    new QParameterNamed1<>(
      "--file",
      List.of(),
      new QStringType.QConstant("The configuration file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Boolean> SELF_TEST =
    new QParameterNamed1<>(
      "--self-test",
      List.of(),
      new QStringType.QConstant("Self-test the server."),
      Optional.of(Boolean.FALSE),
      Boolean.class
    );

  /**
   * Construct a command.
   */

  public LLServer()
  {
    this.metadata = new QCommandMetadata(
      "server",
      new QStringType.QConstant("Start a server."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(
      List.of(FILE, SELF_TEST)
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws Exception
  {
    QLogback.configure(context);

    final var servers =
      new LLServers();
    final var configurations =
      new LLServerConfigurations();

    final var runForever =
      !context.<Boolean>parameterValue(SELF_TEST).booleanValue();

    try (var ignored = servers.open(
      configurations.parse(context.parameterValue(FILE)))) {

      while (runForever) {
        Thread.sleep(1_000L);
      }
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
