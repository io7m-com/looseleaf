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

package com.io7m.looseleaf.server.internal.mx;

import com.io7m.looseleaf.server.internal.services.LLServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * An MX metrics service.
 */

public final class LLMetricsService implements LLServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLMetricsService.class);

  private final LLServerMetricsBean bean;

  /**
   * An MX metrics service.
   */

  public LLMetricsService()
  {
    this.bean = new LLServerMetricsBean();
    this.setupMetrics();
  }

  private void setupMetrics()
  {
    try {
      final var server =
        ManagementFactory.getPlatformMBeanServer();
      final var objectName =
        new ObjectName("com.io7m.looseleaf:name=Metrics");

      server.registerMBean(this.bean, objectName);
    } catch (final MalformedObjectNameException
                   | InstanceAlreadyExistsException
                   | MBeanRegistrationException
                   | NotCompliantMBeanException e) {
      LOG.error("unable to register metrics bean: ", e);
    }
  }

  /**
   * @return The metrics bean
   */

  public LLServerMetricsBean bean()
  {
    return this.bean;
  }

  @Override
  public String description()
  {
    return "MX metrics service.";
  }
}
