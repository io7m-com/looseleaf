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

package com.io7m.looseleaf.server.internal.services;

import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The main service directory implementation.
 */

public final class LLServices implements Closeable
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLServices.class);

  private final Object serviceLock;
  @GuardedBy("serviceLock")
  private final Map<Object, List<Object>> services;

  /**
   * Construct a service directory.
   */

  public LLServices()
  {
    this.serviceLock = new Object();
    this.services = new ConcurrentHashMap<>();
  }

  /**
   * Register the given service.
   *
   * @param clazz   The service interface
   * @param service The service
   * @param <T>     The type of service
   */

  public <T extends LLServiceType> void register(
    final Class<T> clazz,
    final T service)
  {
    Objects.requireNonNull(clazz, "clazz");
    Objects.requireNonNull(service, "service");

    LOG.debug("register: {} → {}", clazz, service);

    synchronized (this.serviceLock) {
      final var existing =
        Optional.ofNullable(this.services.get(clazz))
          .orElse(new ArrayList<>());
      existing.add(service);
      this.services.put(clazz, existing);
    }
  }

  /**
   * Fetch an optional service reference.
   *
   * @param clazz The service class
   * @param <T>   The type of service
   *
   * @return A service, if one is registered
   */

  public <T extends LLServiceType> Optional<T> optionalService(
    final Class<T> clazz)
  {
    Objects.requireNonNull(clazz, "clazz");

    synchronized (this.serviceLock) {
      return Optional.ofNullable(this.services.get(clazz))
        .flatMap(xs -> {
          try {
            return Optional.of(clazz.cast(xs.get(0)));
          } catch (final IndexOutOfBoundsException e) {
            return Optional.empty();
          }
        });
    }
  }

  /**
   * Fetch a required service reference.
   *
   * @param clazz The service class
   * @param <T>   The type of service
   *
   * @return A service, if one is registered
   *
   * @throws LLServiceException If no service is registered of the given type
   */

  public <T extends LLServiceType> T requireService(
    final Class<T> clazz)
    throws LLServiceException
  {
    Objects.requireNonNull(clazz, "clazz");

    return this.optionalService(clazz)
      .orElseThrow(() -> new LLServiceException(
        String.format(
          "No implementations available of type %s",
          clazz.getCanonicalName())
      ));
  }

  /**
   * Fetch a set of optional service references.
   *
   * @param clazz The service class
   * @param <T>   The type of service
   *
   * @return The registered services, if any
   *
   * @throws LLServiceException On errors
   */

  public <T extends LLServiceType> List<? extends T> optionalServices(
    final Class<T> clazz)
    throws LLServiceException
  {
    Objects.requireNonNull(clazz, "clazz");

    synchronized (this.serviceLock) {
      return Optional.ofNullable(this.services.get(clazz))
        .stream()
        .flatMap(xs -> xs.stream().map(clazz::cast))
        .collect(Collectors.toList());
    }
  }

  /**
   * @return The list of registered services
   */

  public List<LLServiceType> services()
  {
    synchronized (this.serviceLock) {
      return this.services.values()
        .stream()
        .flatMap(Collection::stream)
        .map(LLServiceType.class::cast)
        .collect(Collectors.toList());
    }
  }

  @Override
  public void close()
    throws IOException
  {
    final List<Object> allServices;
    synchronized (this.serviceLock) {
      allServices =
        this.services.values()
          .stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }

    Exception exception = null;
    for (final var service : allServices) {
      if (service instanceof AutoCloseable) {
        try {
          LOG.debug("close: {}", service);
          ((AutoCloseable) service).close();
        } catch (final Exception e) {
          if (exception == null) {
            exception = e;
          } else {
            exception.addSuppressed(e);
          }
        }
      }
    }

    if (exception != null) {
      throw new IOException(exception);
    }
  }
}
