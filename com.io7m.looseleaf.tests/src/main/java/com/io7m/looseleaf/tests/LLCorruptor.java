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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.security.SecureRandom;

import static java.nio.file.StandardOpenOption.WRITE;

public final class LLCorruptor
{
  private LLCorruptor()
  {

  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final var file = args[0];

    final var rng =
      SecureRandom.getInstanceStrong();

    try (var channel = FileChannel.open(Paths.get(file), WRITE)) {
      while (true) {
        final var data = new byte[] {
          (byte) (rng.nextInt(256) & 0xff)
        };
        final long position = rng.nextLong(channel.size());
        channel.position(position);
        channel.write(ByteBuffer.wrap(data));

        System.out.printf("0x%x%n", Long.valueOf(position));
        Thread.sleep(10L);
      }
    }
  }
}
