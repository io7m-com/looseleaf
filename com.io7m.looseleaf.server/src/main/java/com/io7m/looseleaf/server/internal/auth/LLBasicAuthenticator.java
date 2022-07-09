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

package com.io7m.looseleaf.server.internal.auth;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.DeferredAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.util.security.Constraint;

import java.io.IOException;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An authenticator for basic authentication.
 */

public final class LLBasicAuthenticator implements Authenticator
{
  private IdentityService identityService;
  private LoginService loginService;

  /**
   * An authenticator for basic authentication.
   */

  public LLBasicAuthenticator()
  {

  }

  private static String decodeBase64String(
    final Base64.Decoder base64,
    final String credentialsPtr)
  {
    // CHECKSTYLE:OFF
    return new String(base64.decode(credentialsPtr), UTF_8);
    // CHECKSTYLE:ON
  }

  @Override
  public void setConfiguration(
    final AuthConfiguration configuration)
  {
    this.loginService = configuration.getLoginService();
    if (this.loginService == null) {
      throw new IllegalStateException(
        "No LoginService for %s in %s".formatted(this, configuration));
    }

    this.identityService = configuration.getIdentityService();
    if (this.identityService == null) {
      throw new IllegalStateException(
        "No IdentityService for %s in %s".formatted(this, configuration));
    }
  }

  @Override
  public String getAuthMethod()
  {
    return Constraint.__BASIC_AUTH;
  }

  @Override
  public void prepareRequest(
    final ServletRequest request)
  {
    // Nothing to do
  }

  @Override
  public Authentication validateRequest(
    final ServletRequest req,
    final ServletResponse res,
    final boolean mandatory)
    throws ServerAuthException
  {
    if (!mandatory) {
      return Authentication.NOT_CHECKED;
    }

    final var request =
      (HttpServletRequest) req;
    final var response =
      (HttpServletResponse) res;

    final var credentials =
      request.getHeader(HttpHeader.AUTHORIZATION.asString());

    try {
      if (credentials != null) {
        final UserAuthentication user =
          this.parseAndLogIn(request, credentials);
        if (user != null) {
          return user;
        }
      }

      if (DeferredAuthentication.isDeferred(response)) {
        return Authentication.UNAUTHENTICATED;
      }

      final var value =
        "basic realm=\"%s\", charset=\"%s\""
          .formatted(this.loginService.getName(), UTF_8.name());

      response.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), value);
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return Authentication.SEND_CONTINUE;
    } catch (final IOException e) {
      throw new ServerAuthException(e);
    }
  }

  private UserAuthentication parseAndLogIn(
    final HttpServletRequest request,
    final String credentials)
  {
    final var space =
      credentials.indexOf(' ');
    final var base64 =
      Base64.getDecoder();

    if (space > 0) {
      final var method = credentials.substring(0, space);
      if ("basic".equalsIgnoreCase(method)) {
        var credentialsPtr = credentials;
        credentialsPtr = credentialsPtr.substring(space + 1);
        credentialsPtr = decodeBase64String(base64, credentialsPtr);

        final var i = credentialsPtr.indexOf(':');
        if (i > 0) {
          final var username =
            credentialsPtr.substring(0, i);
          final var password =
            credentialsPtr.substring(i + 1);

          return this.logIn(request, username, password);
        }
      }
    }
    return null;
  }

  private UserAuthentication logIn(
    final HttpServletRequest request,
    final String username,
    final String password)
  {
    final var user =
      this.loginService.login(username, password, request);

    if (user != null) {
      return new UserAuthentication(this.getAuthMethod(), user);
    }
    return null;
  }

  @Override
  public boolean secureResponse(
    final ServletRequest request,
    final ServletResponse response,
    final boolean mandatory,
    final Authentication.User validatedUser)
  {
    return true;
  }
}
