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


package com.io7m.looseleaf.protocol.v1;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The base type of {@code looseleaf} messages.
 */

@JsonSubTypes(
  value = {
    @JsonSubTypes.Type(
      value = LLv1Error.class,
      name = "LLv1Error"),
    @JsonSubTypes.Type(
      value = LLv1Errors.class,
      name = "LLv1Errors"),
    @JsonSubTypes.Type(
      value = LLv1RUD.class,
      name = "LLv1RUD"),
    @JsonSubTypes.Type(
      value = LLv1Result.class,
      name = "LLv1Result")
  }
)
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, visible = false)
public sealed interface LLv1MessageType
  permits LLv1Error, LLv1Errors, LLv1RUD, LLv1Result
{

}
