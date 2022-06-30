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
import com.io7m.looseleaf.security.LLGrant;
import com.io7m.looseleaf.security.LLKeyExpression;

import java.util.Objects;

/**
 * A grant of permission to perform an action on a set of keys.
 *
 * @param action The action
 * @param keys   The key expression
 */

@JsonDeserialize
@JsonSerialize
public record LLServerGrant(
  @JsonProperty(value = "action", required = true)
  LLServerAction action,
  @JsonProperty(value = "keys", required = true)
  String keys)
{
  /**
   * A grant of permission to perform an action on a set of keys.
   *
   * @param action The action
   * @param keys   The key expression
   */

  public LLServerGrant
  {
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(keys, "keys");
  }

  /**
   * @return The configuration as a grant
   */

  public LLGrant asGrant()
  {
    return new LLGrant(
      this.action.asAction(),
      LLKeyExpression.create(this.keys)
    );
  }
}
