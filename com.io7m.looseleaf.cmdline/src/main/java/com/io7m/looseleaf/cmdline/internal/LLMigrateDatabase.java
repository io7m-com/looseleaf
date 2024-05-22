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

import com.io7m.looseleaf.database.api.LLDatabaseFactoryType;
import com.io7m.looseleaf.database.api.LLDatabaseRUD;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Migrate data between databases.
 */

public final class LLMigrateDatabase implements QCommandType
{
  private static final QParameterNamed1<Path> DATABASE_SOURCE =
    new QParameterNamed1<>(
      "--database-source",
      List.of(),
      new QStringType.QConstant("The source database."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> DATABASE_TARGET =
    new QParameterNamed1<>(
      "--database-target",
      List.of(),
      new QStringType.QConstant("The target database."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<String> DATABASE_SOURCE_KIND =
    new QParameterNamed1<>(
      "--database-source-kind",
      List.of(),
      new QStringType.QConstant("The source database kind."),
      Optional.empty(),
      String.class
    );

  private static final QParameterNamed1<String> DATABASE_TARGET_KIND =
    new QParameterNamed1<>(
      "--database-target-kind",
      List.of(),
      new QStringType.QConstant("The target database kind."),
      Optional.empty(),
      String.class
    );

  private final QCommandMetadata metadata;

  /**
   * Construct a command.
   */

  public LLMigrateDatabase()
  {
    this.metadata = new QCommandMetadata(
      "migrate-database",
      new QStringType.QConstant("Migrate data between databases."),
      Optional.empty()
    );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(List.of(
      DATABASE_SOURCE,
      DATABASE_SOURCE_KIND,
      DATABASE_TARGET,
      DATABASE_TARGET_KIND)
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws IOException
  {
    QLogback.configure(context);

    final var srcFile =
      context.parameterValue(DATABASE_SOURCE);
    final var srcKind =
      context.parameterValue(DATABASE_SOURCE_KIND);
    final var targetFile =
      context.parameterValue(DATABASE_TARGET);
    final var targetKind =
      context.parameterValue(DATABASE_TARGET_KIND);

    final var databases =
      ServiceLoader.load(LLDatabaseFactoryType.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .toList();

    final var srcDatabases =
      databases.stream()
        .filter(x -> Objects.equals(x.kind(), srcKind))
        .findFirst()
        .orElseThrow(() -> {
          return new UnsupportedOperationException(
            "No database implementation available with kind '%s'"
              .formatted(srcKind)
          );
        });

    final var targetDatabases =
      databases.stream()
        .filter(x -> Objects.equals(x.kind(), targetKind))
        .findFirst()
        .orElseThrow(() -> {
          return new UnsupportedOperationException(
            "No database implementation available with kind '%s'"
              .formatted(targetKind)
          );
        });

    try (var srcDatabase = srcDatabases.open(srcFile);
         var targetDatabase = targetDatabases.open(targetFile)) {
      final var all = srcDatabase.getAll();
      targetDatabase.readUpdateDelete(
        new LLDatabaseRUD(
          Set.of(),
          all,
          Set.of()
        )
      );
    }

    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
