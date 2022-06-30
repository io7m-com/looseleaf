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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.io7m.looseleaf.security.LLPassword;
import com.io7m.looseleaf.security.LLPasswordAlgorithms;
import com.io7m.looseleaf.security.LLPasswordException;

import java.util.Objects;

/**
 * A hashed password.
 *
 * @param algorithm The password algorithm identifier
 * @param salt      The password salt
 * @param hash      The password hash
 */

@JsonDeserialize
@JsonSerialize
public record LLServerHashedPassword(
  @JsonProperty(value = "algorithm", required = true)
  String algorithm,
  @JsonProperty(value = "salt", required = true)
  String salt,
  @JsonProperty(value = "hash", required = true)
  String hash)
{
  /**
   * A hashed password.
   *
   * @param algorithm The password algorithm identifier
   * @param salt      The password salt
   * @param hash      The password hash
   */

  public LLServerHashedPassword
  {
    Objects.requireNonNull(algorithm, "algorithm");
    Objects.requireNonNull(salt, "salt");
    Objects.requireNonNull(hash, "hash");
  }

  /**
   * @return The configuration as a password
   *
   * @throws LLPasswordException On errors
   */

  public LLPassword asPassword()
    throws LLPasswordException
  {
    return new LLPassword(
      LLPasswordAlgorithms.parse(this.algorithm),
      this.hash,
      this.salt
    );
  }
}
