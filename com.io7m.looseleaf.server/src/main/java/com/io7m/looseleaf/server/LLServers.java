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

package com.io7m.looseleaf.server;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.looseleaf.database.mvstore.LLDatabaseMVStoreFactory;
import com.io7m.looseleaf.security.LLSecurityContext;
import com.io7m.looseleaf.server.api.LLServerAddress;
import com.io7m.looseleaf.server.api.LLServerConfiguration;
import com.io7m.looseleaf.server.api.LLServerFactoryType;
import com.io7m.looseleaf.server.api.LLServerType;
import com.io7m.looseleaf.server.internal.LLConfigurationService;
import com.io7m.looseleaf.server.internal.LLDatabaseService;
import com.io7m.looseleaf.server.internal.LLErrorHandler;
import com.io7m.looseleaf.server.internal.LLHealth;
import com.io7m.looseleaf.server.internal.LLMetricsService;
import com.io7m.looseleaf.server.internal.LLRequestLogger;
import com.io7m.looseleaf.server.internal.LLServer;
import com.io7m.looseleaf.server.internal.LLServerClock;
import com.io7m.looseleaf.server.internal.LLServerRequestDecoration;
import com.io7m.looseleaf.server.internal.LLServerRequestTimeFilter;
import com.io7m.looseleaf.server.internal.LLServletHolders;
import com.io7m.looseleaf.server.internal.LLStrings;
import com.io7m.looseleaf.server.internal.LLVersions;
import com.io7m.looseleaf.server.internal.LLv1MessagesService;
import com.io7m.looseleaf.server.internal.auth.LLBasicAuthenticator;
import com.io7m.looseleaf.server.internal.auth.LLLoginService;
import com.io7m.looseleaf.server.internal.telemetry.LLTelemetryServiceType;
import com.io7m.looseleaf.server.internal.telemetry.LLTelemetryServices;
import com.io7m.looseleaf.server.internal.v1.LLCheckAuthServlet;
import com.io7m.looseleaf.server.internal.v1.LLDeleteServlet;
import com.io7m.looseleaf.server.internal.v1.LLRUDServlet;
import com.io7m.looseleaf.server.internal.v1.LLReadServlet;
import com.io7m.looseleaf.server.internal.v1.LLUpdateServlet;
import com.io7m.repetoir.core.RPServiceDirectory;
import com.io7m.repetoir.core.RPServiceDirectoryType;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;

import static jakarta.servlet.DispatcherType.REQUEST;

/**
 * A server factory.
 */

public final class LLServers implements LLServerFactoryType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLServers.class);

  private final LLDatabaseMVStoreFactory databases;

  /**
   * A server factory.
   */

  public LLServers()
  {
    this.databases = new LLDatabaseMVStoreFactory();
  }

  private static ConstraintSecurityHandler createSecurityHandler(
    final LLTelemetryServiceType telemetry,
    final LLSecurityContext context,
    final LLMetricsService metrics)
  {
    final var loginService = new LLLoginService(context);
    loginService.setName("looseleaf");

    final var securityHandler = new ConstraintSecurityHandler();
    securityHandler.setAuthenticator(
      new LLBasicAuthenticator(telemetry, metrics)
    );
    securityHandler.setRealmName("looseleaf");
    securityHandler.setLoginService(loginService);

    /*
     * Add a constraint that basic authentication is required, and
     * apply the constraint to /v1/* endpoints.
     */

    final var constraint = new Constraint();
    constraint.setName(Constraint.__BASIC_AUTH);
    constraint.setRoles(new String[]{"user"});
    constraint.setAuthenticate(true);

    final var v1ConstraintMapping = new ConstraintMapping();
    v1ConstraintMapping.setConstraint(constraint);
    v1ConstraintMapping.setPathSpec("/v1/*");

    securityHandler.addConstraintMapping(v1ConstraintMapping);
    return securityHandler;
  }

  @Override
  public LLServerType open(
    final LLServerConfiguration configuration)
    throws IOException
  {
    Objects.requireNonNull(configuration, "configuration");

    final var securityContext =
      configuration.toSecurityContext();
    final var resources =
      CloseableCollection.create(IOException::new);
    final var exceptions =
      new ExceptionTracker<IOException>();
    final var database =
      resources.add(this.databases.open(configuration.databaseFile()));

    final var services =
      new RPServiceDirectory();
    final var telemetry =
      LLTelemetryServices.createOptional(configuration.telemetry());

    services.register(
      LLConfigurationService.class,
      new LLConfigurationService(configuration)
    );

    services.register(LLTelemetryServiceType.class, telemetry);
    services.register(
      LLServerClock.class,
      new LLServerClock(Clock.systemUTC()));

    final var databaseService = new LLDatabaseService(database);
    services.register(LLDatabaseService.class, databaseService);

    services.register(LLv1MessagesService.class, new LLv1MessagesService());
    services.register(LLStrings.class, new LLStrings(Locale.getDefault()));

    services.register(
      LLMetricsService.class,
      new LLMetricsService(databaseService, telemetry)
    );

    final var servers = new ArrayList<Server>();
    for (final var address : configuration.addresses()) {
      try {
        final var server =
          this.createServer(services, securityContext, address);
        resources.add(server::stop);
        servers.add(server);
      } catch (final Exception e) {
        exceptions.addException(new IOException(e));
      }
    }

    exceptions.throwIfNecessary();
    return new LLServer(resources, servers, services);
  }

  private Server createServer(
    final RPServiceDirectoryType services,
    final LLSecurityContext securityContext,
    final LLServerAddress address)
    throws Exception
  {
    final var server =
      new Server(new InetSocketAddress(address.host(), address.port()));

    /*
     * Configure all the servlets.
     */

    final var servletHolders =
      new LLServletHolders(services);
    final var servlets =
      new ServletContextHandler();

    servlets.setSecurityHandler(
      createSecurityHandler(
        services.requireService(LLTelemetryServiceType.class),
        securityContext,
        services.requireService(LLMetricsService.class))
    );

    servlets.addServlet(
      servletHolders.create(LLVersions.class, LLVersions::new),
      "/"
    );
    servlets.addServlet(
      servletHolders.create(LLHealth.class, LLHealth::new),
      "/health"
    );

    /*
     * Version 1 protocol.
     */

    servlets.addServlet(
      servletHolders.create(LLRUDServlet.class, LLRUDServlet::new),
      "/v1/rud"
    );
    servlets.addServlet(
      servletHolders.create(LLUpdateServlet.class, LLUpdateServlet::new),
      "/v1/update/*"
    );
    servlets.addServlet(
      servletHolders.create(LLReadServlet.class, LLReadServlet::new),
      "/v1/read/*"
    );
    servlets.addServlet(
      servletHolders.create(LLDeleteServlet.class, LLDeleteServlet::new),
      "/v1/delete/*"
    );
    servlets.addServlet(
      servletHolders.create(LLCheckAuthServlet.class, LLCheckAuthServlet::new),
      "/v1/check-auth"
    );

    /*
     * Add a handler that tracks request/response time.
     */

    final var filterHolder =
      new FilterHolder(
        new LLServerRequestTimeFilter(
          services.requireService(LLMetricsService.class),
          services.requireService(LLServerClock.class)
        )
      );

    servlets.addFilter(filterHolder, "*", EnumSet.of(REQUEST));

    /*
     * Add a connector listener that adds unique identifiers to all requests.
     */

    Arrays.stream(server.getConnectors()).forEach(
      connector -> connector.addBean(new LLServerRequestDecoration(services))
    );

    server.setRequestLog(new LLRequestLogger(
      services.requireService(LLMetricsService.class)
    ));
    server.setErrorHandler(new LLErrorHandler());
    server.setHandler(servlets);
    server.start();
    LOG.info("[{}:{}] server started", address.host(), address.port());
    return server;
  }
}
