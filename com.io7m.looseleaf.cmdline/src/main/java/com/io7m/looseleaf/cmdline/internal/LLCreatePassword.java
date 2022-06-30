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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.io7m.claypot.core.CLPAbstractCommand;
import com.io7m.claypot.core.CLPCommandContextType;
import com.io7m.looseleaf.security.LLPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.looseleaf.server.api.LLServerHashedPassword;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS;

/**
 * Create a hashed password.
 */

@Parameters(commandDescription = "Create a hashed password.")
public final class LLCreatePassword extends CLPAbstractCommand
{
  @Parameter(
    names = "--password",
    description = "The password",
    required = true
  )
  private String password;

  /**
   * Construct a command.
   *
   * @param inContext The command context
   */

  public LLCreatePassword(
    final CLPCommandContextType inContext)
  {
    super(inContext);
  }

  @Override
  protected Status executeActual()
    throws Exception
  {
    final var algorithm =
      LLPasswordAlgorithmPBKDF2HmacSHA256.create();
    final var hashedPassword =
      algorithm.createHashed(this.password);

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

    mapper.writeValue(System.out, configuration);
    return Status.SUCCESS;
  }

  @Override
  public String name()
  {
    return "create-password";
  }
}
