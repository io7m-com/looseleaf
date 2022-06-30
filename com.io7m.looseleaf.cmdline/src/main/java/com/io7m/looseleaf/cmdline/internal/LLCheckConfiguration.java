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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.looseleaf.server.api.LLServerConfigurations;

import java.nio.file.Path;

/**
 * Check configuration file.
 */

@Parameters(commandDescription = "Check configuration file.")
public final class LLCheckConfiguration extends CLPAbstractCommand
{
  @Parameter(
    names = "--file",
    description = "The configuration file",
    required = true
  )
  private Path file;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public LLCheckConfiguration(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
  {
    try {
      final var configurations =
        new LLServerConfigurations();
      final var configuration =
        configurations.parse(this.file);

      configuration.toSecurityContext();
      return Status.SUCCESS;
    } catch (final Exception e) {
      this.logger().error("{}", e.getMessage());
      return Status.FAILURE;
    }
  }

  @Override
  public String name()
  {
    return "check-configuration";
  }
}
