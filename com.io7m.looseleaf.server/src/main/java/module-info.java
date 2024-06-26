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
 * Minimalist transactional HTTP key/value store (Server)
 */

module com.io7m.looseleaf.server
{
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;

  requires com.io7m.looseleaf.server.api;
  requires com.io7m.looseleaf.database.api;
  requires com.io7m.looseleaf.database.mvstore;
  requires com.io7m.looseleaf.database.sqlite;
  requires com.io7m.looseleaf.protocol.v1;

  requires com.fasterxml.jackson.databind;
  requires com.io7m.jcip.annotations;
  requires com.io7m.jdeferthrow.core;
  requires com.io7m.jmulticlose.core;
  requires com.io7m.jxtrand.vanilla;
  requires com.io7m.repetoir.core;
  requires java.management;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.servlet;
  requires org.slf4j;

  requires io.opentelemetry.api;
  requires io.opentelemetry.context;
  requires io.opentelemetry.sdk.logs;
  requires io.opentelemetry.sdk.common;
  requires io.opentelemetry.sdk.metrics;
  requires io.opentelemetry.sdk.trace;
  requires io.opentelemetry.semconv;
  requires io.opentelemetry.exporter.otlp;
  requires io.opentelemetry.sdk;

  exports com.io7m.looseleaf.server;
  opens com.io7m.looseleaf.server.internal;
}
