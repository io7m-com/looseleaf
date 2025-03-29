/*
 * Copyright © 2024 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

import com.io7m.looseleaf.database.api.LLDatabaseRUD;
import com.io7m.looseleaf.database.api.LLDatabaseType;
import com.io7m.looseleaf.security.LLKeyName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class LLDatabaseContract
{
  private LLDatabaseType database;

  protected abstract LLDatabaseType create(Path file)
    throws IOException;

  @BeforeEach
  public void setup(
    final @TempDir Path directory)
    throws Exception
  {
    this.database =
      this.create(directory.resolve("database.db"));
  }

  @AfterEach
  public void tearDown()
    throws IOException
  {
    this.database.close();
  }

  @Test
  public void testEmpty0()
    throws Exception
  {
    assertEquals(0L, this.database.keyCountApproximate());
    assertEquals(Map.of(), this.database.getAll());
  }

  @Test
  public void testRud0()
    throws Exception
  {
    assertEquals(
      Map.of(),
      this.database.readUpdateDelete(
        new LLDatabaseRUD(
          Set.of(),
          Map.of(),
          Set.of()
        )
      ));

    assertEquals(0L, this.database.keyCountApproximate());
    assertEquals(Map.of(), this.database.getAll());
  }

  @Test
  public void testRud1()
    throws Exception
  {
    final var k0 =
      LLKeyName.create("/a/b/c");
    final var k1 =
      LLKeyName.create("/x/y/z");

    assertEquals(
      Map.of(),
      this.database.readUpdateDelete(
        new LLDatabaseRUD(
          Set.of(),
          Map.ofEntries(
            Map.entry(k0, "ABC!"),
            Map.entry(k1, "XYZ!")
          ),
          Set.of()
        )
      ));

    assertEquals(2L, this.database.keyCountApproximate());
    assertEquals(
      Map.ofEntries(
        Map.entry(k0, "ABC!"),
        Map.entry(k1, "XYZ!")
      ),
      this.database.getAll()
    );

    assertEquals("ABC!", this.database.get(k0).orElseThrow());
    assertEquals("XYZ!", this.database.get(k1).orElseThrow());

    assertEquals(
      Map.of(),
      this.database.readUpdateDelete(
        new LLDatabaseRUD(
          Set.of(),
          Map.of(),
          Set.of(k0)
        )
      ));

    assertEquals(1L, this.database.keyCountApproximate());
    assertEquals(
      Map.ofEntries(
        Map.entry(k1, "XYZ!")
      ),
      this.database.getAll()
    );

    assertEquals(Optional.empty(), this.database.get(k0));
    assertEquals("XYZ!", this.database.get(k1).orElseThrow());
  }
}
