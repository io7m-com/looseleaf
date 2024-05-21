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


package com.io7m.looseleaf.tests;

import com.io7m.looseleaf.cmdline.internal.LLCheckConfiguration;
import com.io7m.looseleaf.cmdline.internal.LLCreatePassword;
import com.io7m.looseleaf.cmdline.internal.LLMigrateDatabase;
import com.io7m.looseleaf.cmdline.internal.LLServer;
import com.io7m.quarrel.core.QCommandOrGroupType;
import com.io7m.quarrel.core.QCommandParserConfiguration;
import com.io7m.quarrel.core.QCommandParsers;
import com.io7m.quarrel.core.QValueConverterDirectory;
import com.io7m.quarrel.ext.xstructural.QCommandXS;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class LLCommandsDocumentation
{
  private LLCommandsDocumentation()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var parsers =
      new QCommandParsers();
    final var xs =
      new QCommandXS("xs", true);

    final var commands =
      List.of(
        new LLCheckConfiguration(),
        new LLCreatePassword(),
        new LLServer(),
        new LLMigrateDatabase()
      );

    final var byName =
      new TreeMap<String, QCommandOrGroupType>(
        commands.stream()
          .map(c -> Map.entry(c.metadata().name(), c))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );

    final var parserConfig =
      new QCommandParserConfiguration(
        QValueConverterDirectory.core(),
        QCommandParsers.emptyResources()
      );

    for (final var command : commands) {
      try {
        final var name =
          command.metadata().name();
        final var parser =
          parsers.create(parserConfig);

        final var textWriter =
          new StringWriter();
        final var writer =
          new PrintWriter(textWriter);

        final var context =
          parser.execute(
            byName,
            writer,
            xs,
            List.of("--type", "main", name)
          );

        context.execute();
        writer.println();
        writer.flush();

        final var path =
          Paths.get("/shared-tmp/ll-%s.xml".formatted(name));

        Files.writeString(
          path,
          textWriter.toString(),
          StandardCharsets.UTF_8);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    for (final var command : commands) {
      try {
        final var name =
          command.metadata().name();
        final var parser =
          parsers.create(parserConfig);

        final var textWriter =
          new StringWriter();
        final var writer =
          new PrintWriter(textWriter);

        final var context =
          parser.execute(
            byName,
            writer,
            xs,
            List.of("--type", "parameters", name)
          );

        context.execute();
        writer.println();
        writer.flush();

        final var path =
          Paths.get("/shared-tmp/ll-parameters-%s.xml".formatted(name));

        Files.writeString(
          path,
          textWriter.toString(),
          StandardCharsets.UTF_8);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}
