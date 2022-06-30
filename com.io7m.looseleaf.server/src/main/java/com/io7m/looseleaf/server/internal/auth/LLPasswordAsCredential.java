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


package com.io7m.looseleaf.server.internal.auth;

import com.io7m.looseleaf.security.LLPassword;
import com.io7m.looseleaf.security.LLPasswordException;
import org.eclipse.jetty.util.security.Credential;

import java.util.Objects;

/**
 * A credential implementation that contains a hashed looseleaf password.
 */

public final class LLPasswordAsCredential extends Credential
{
  private final LLPassword password;

  /**
   * A credential implementation that contains a hashed looseleaf password.
   *
   * @param inPassword The password
   */

  public LLPasswordAsCredential(
    final LLPassword inPassword)
  {
    this.password = Objects.requireNonNull(inPassword, "password");
  }

  @Override
  public boolean check(
    final Object credentials)
  {
    if (credentials instanceof String text) {
      try {
        return this.password.check(text);
      } catch (final LLPasswordException e) {
        return false;
      }
    }
    return false;
  }
}
