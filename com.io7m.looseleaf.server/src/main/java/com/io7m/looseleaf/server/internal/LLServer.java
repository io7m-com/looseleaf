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

package com.io7m.looseleaf.server.internal;

import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.looseleaf.server.api.LLServerType;
import com.io7m.looseleaf.server.internal.services.LLServices;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * The main server implementation.
 */

public final class LLServer implements LLServerType
{
  private final CloseableCollectionType<IOException> resources;
  private final ArrayList<Server> servers;
  private final LLServices services;

  /**
   * The main server implementation.
   *
   * @param inServices  The service directory
   * @param inServers   The Jetty servers
   * @param inResources The server resources
   */

  public LLServer(
    final CloseableCollectionType<IOException> inResources,
    final ArrayList<Server> inServers,
    final LLServices inServices)
  {
    this.resources =
      Objects.requireNonNull(inResources, "resources");
    this.servers =
      Objects.requireNonNull(inServers, "servers");
    this.services =
      Objects.requireNonNull(inServices, "services");
  }

  @Override
  public void close()
    throws IOException
  {
    this.resources.close();
  }
}
