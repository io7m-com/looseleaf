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

/**
 * Minimalist transactional HTTP key/value store (Test suite)
 */

open module com.io7m.looseleaf.tests
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.looseleaf.cmdline;
  requires com.io7m.looseleaf.database.api;
  requires com.io7m.looseleaf.database.mvstore;
  requires com.io7m.looseleaf.database.sqlite;
  requires com.io7m.looseleaf.grafana;
  requires com.io7m.looseleaf.protocol.v1;
  requires com.io7m.looseleaf.security;
  requires com.io7m.looseleaf.server.api;
  requires com.io7m.looseleaf.server;

  requires com.io7m.quarrel.core;
  requires com.io7m.quarrel.ext.xstructural;
  requires java.net.http;
  requires org.slf4j;

  exports com.io7m.looseleaf.tests;

  requires org.junit.jupiter.api;
  requires org.junit.jupiter.engine;
  requires org.junit.platform.commons;
  requires org.junit.platform.engine;
  requires org.junit.platform.launcher;
}
