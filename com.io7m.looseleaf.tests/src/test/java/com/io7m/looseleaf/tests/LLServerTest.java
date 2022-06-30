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


package com.io7m.looseleaf.tests;

import com.io7m.looseleaf.protocol.v1.LLv1Errors;
import com.io7m.looseleaf.protocol.v1.LLv1Messages;
import com.io7m.looseleaf.protocol.v1.LLv1Result;
import com.io7m.looseleaf.security.LLPassword;
import com.io7m.looseleaf.security.LLPasswordAlgorithmPBKDF2HmacSHA256;
import com.io7m.looseleaf.server.LLServers;
import com.io7m.looseleaf.server.api.LLServerAddress;
import com.io7m.looseleaf.server.api.LLServerConfiguration;
import com.io7m.looseleaf.server.api.LLServerGrant;
import com.io7m.looseleaf.server.api.LLServerHashedPassword;
import com.io7m.looseleaf.server.api.LLServerRole;
import com.io7m.looseleaf.server.api.LLServerType;
import com.io7m.looseleaf.server.api.LLServerUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.io7m.looseleaf.server.api.LLServerAction.READ;
import static com.io7m.looseleaf.server.api.LLServerAction.WRITE;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class LLServerTest
{
  private LLServers servers;
  private Path directory;
  private LLPassword password0;
  private LLServerType server;
  private HttpClient client;
  private LLv1Messages messages;

  private static URI uriOf(final String path)
  {
    return URI.create("http://localhost:20000" + path);
  }

  private static String basic(
    final String user,
    final String pass)
  {
    return "Basic " + base64(user + ":" + pass);
  }

  private static String base64(
    final String text)
  {
    return Base64.getUrlEncoder().encodeToString(text.getBytes(UTF_8));
  }

  @BeforeEach
  public void setup()
    throws Exception
  {
    this.directory =
      LLTestDirectories.createTempDirectory();
    this.messages =
      new LLv1Messages();
    this.password0 =
      LLPasswordAlgorithmPBKDF2HmacSHA256.create()
        .createHashed("password0");

    this.servers = new LLServers();
    this.server = this.servers.open(
      new LLServerConfiguration(
        null,
        List.of(new LLServerAddress("localhost", 20000)),
        this.directory.resolve("looseleaf.db"),
        List.of(
          new LLServerRole(
            "xy-reader",
            List.of(new LLServerGrant(READ, "/x/y/*"))
          ),
          new LLServerRole(
            "xy-writer",
            List.of(new LLServerGrant(WRITE, "/x/y/*"))
          )
        ),
        List.of(
          new LLServerUser(
            "grouch",
            new LLServerHashedPassword(
              this.password0.algorithm().identifier(),
              this.password0.salt(),
              this.password0.hash()
            ),
            List.of("xy-reader", "xy-writer")
          )
        )
      )
    );

    this.client = HttpClient.newHttpClient();
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.server.close();
    LLTestDirectories.deleteDirectory(this.directory);
  }

  /**
   * Failing to authenticate results in failure.
   *
   * @throws Exception On errors
   */

  @Test
  public void testNotAuthenticated()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/check-auth"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(401, response.statusCode());
  }

  /**
   * Authenticating works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuthenticated()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/check-auth"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(200, response.statusCode());
  }

  /**
   * Authenticating fails for nonexistent users.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuthenticateUserNonexistent()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/check-auth"))
        .header("Authorization", basic("grouch1", "password0"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(401, response.statusCode());
  }

  /**
   * Authenticating fails for bad passwords.
   *
   * @throws Exception On errors
   */

  @Test
  public void testAuthenticateUserBadPassword()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/check-auth"))
        .header("Authorization", basic("grouch", "what"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(401, response.statusCode());
  }

  /**
   * Getting a nonexistent key results in 404.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadNonexistent()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/nonexistent"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(404, response.statusCode());
  }

  /**
   * Getting a key to which the user has no access results in 400.
   *
   * @throws Exception On errors
   */

  @Test
  public void testReadNotPermitted()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/read/a/b/c"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(400, response.statusCode());
  }

  /**
   * Updating a key to which the user has no access results in 400.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateNotPermitted()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/update/a/b/c"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(400, response.statusCode());
  }

  /**
   * Deleting a key to which the user has no access results in 400.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDeleteNotPermitted()
    throws Exception
  {
    final var request =
      HttpRequest.newBuilder(uriOf("/v1/delete/a/b/c"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var response =
      this.client.send(request, ofByteArray());

    assertEquals(400, response.statusCode());
  }

  /**
   * Getting/setting keys works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testUpdateRead()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/update/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString("Hello."))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    assertEquals(200, res0.statusCode());

    final var req1 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res1 =
      this.client.send(req1, ofByteArray());

    assertEquals(200, res1.statusCode());
    assertEquals("Hello.", new String(res1.body(), UTF_8));
  }

  /**
   * Deleting/reading keys fails in the expected way.
   *
   * @throws Exception On errors
   */

  @Test
  public void testDeleteRead()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/update/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString("Hello."))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    assertEquals(200, res0.statusCode());

    final var req1 =
      HttpRequest.newBuilder(uriOf("/v1/delete/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res1 =
      this.client.send(req1, ofByteArray());

    assertEquals(200, res1.statusCode());

    final var req2 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res2 =
      this.client.send(req2, ofByteArray());

    assertEquals(404, res2.statusCode());
  }

  /**
   * RUD works.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUD()
    throws Exception
  {
    {
      final var req0 =
        HttpRequest.newBuilder(uriOf("/v1/update/x/y/a"))
          .header("Authorization", basic("grouch", "password0"))
          .POST(ofString("Hello."))
          .build();

      final var res0 =
        this.client.send(req0, ofByteArray());

      assertEquals(200, res0.statusCode());
    }

    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString(
          """
            {
              "read": [
                "/x/y/z"
              ],
              "update": {
                "/x/y/z": "23"
              },
              "delete": [
                "/x/y/a"
              ]
            }                   
                        """))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    assertEquals(200, res0.statusCode());

    final var results =
      (LLv1Result) this.messages.deserialize(res0.body());
    assertEquals(Map.of(), results.values());

    final var req1 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res1 =
      this.client.send(req1, ofByteArray());

    assertEquals(200, res1.statusCode());
    assertEquals("23", new String(res1.body(), UTF_8));

    final var req2 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/a"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res2 =
      this.client.send(req2, ofByteArray());

    assertEquals(404, res2.statusCode());
  }

  /**
   * RUD respects read permissions and is atomic.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUDReadNotPermitted()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString(
          """
            {
              "read": [
                "/a/b/c"
              ],
              "update": {
                "/x/y/z": "23"
              },
              "delete": [
              
              ]
            }                   
                        """))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    final var errors =
      (LLv1Errors) this.messages.deserialize(res0.body());
    assertEquals(400, res0.statusCode());
    assertEquals("operation-not-permitted", errors.errors().get(0).errorCode());

    final var req1 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res1 =
      this.client.send(req1, ofByteArray());

    assertEquals(404, res1.statusCode());
  }

  /**
   * RUD rejects bad keys.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUDBadKeys()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString(
          """
            {
              "read": [
                "wrong!"
              ],
              "update": {
                "wrong!": "23"
              },
              "delete": [
                "wrong!"
              ]
            }                   
                        """))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    final var errors =
      (LLv1Errors) this.messages.deserialize(res0.body());
    assertEquals(400, res0.statusCode());
    assertEquals("bad-key-name", errors.errors().get(0).errorCode());
    assertEquals("bad-key-name", errors.errors().get(1).errorCode());
    assertEquals("bad-key-name", errors.errors().get(2).errorCode());
  }

  /**
   * RUD respects write permissions and is atomic.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUDWriteNotPermitted()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString(
          """
            {
              "read": [],
              "update": {
                "/x/y/z": "23",
                "/a/b/c": "23"
              },
              "delete": [
              
              ]
            }                   
                        """))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    final var errors =
      (LLv1Errors) this.messages.deserialize(res0.body());
    assertEquals(400, res0.statusCode());
    assertEquals("operation-not-permitted", errors.errors().get(0).errorCode());

    final var req1 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res1 =
      this.client.send(req1, ofByteArray());

    assertEquals(404, res1.statusCode());
  }

  /**
   * RUD respects write (delete) permissions and is atomic.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUDDeleteNotPermitted()
    throws Exception
  {
    {
      final var req0 =
        HttpRequest.newBuilder(uriOf("/v1/update/x/y/z"))
          .header("Authorization", basic("grouch", "password0"))
          .POST(ofString("Hello."))
          .build();

      final var res0 =
        this.client.send(req0, ofByteArray());

      assertEquals(200, res0.statusCode());
    }

    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString(
          """
            {
              "read": [],
              "update": {},
              "delete": [
                "/x/y/z",
                "/a/b/c"
              ]
            }                
                        """))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    final var errors =
      (LLv1Errors) this.messages.deserialize(res0.body());
    assertEquals(400, res0.statusCode());
    assertEquals("operation-not-permitted", errors.errors().get(0).errorCode());

    final var req1 =
      HttpRequest.newBuilder(uriOf("/v1/read/x/y/z"))
        .header("Authorization", basic("grouch", "password0"))
        .build();

    final var res1 =
      this.client.send(req1, ofByteArray());

    assertEquals(200, res1.statusCode());
  }

  /**
   * RUD rejects garbage messages.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUDRejectsGarbage()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString("{}"))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    final var errors =
      (LLv1Errors) this.messages.deserialize(res0.body());
    assertEquals(400, res0.statusCode());
    assertEquals("bad-message", errors.errors().get(0).errorCode());
  }

  /**
   * RUD rejects unexpected messages.
   *
   * @throws Exception On errors
   */

  @Test
  public void testRUDRejectsUnexpected()
    throws Exception
  {
    final var req0 =
      HttpRequest.newBuilder(uriOf("/v1/rud"))
        .header("Authorization", basic("grouch", "password0"))
        .POST(ofString("""
                         {"values":{"/x/y/z":"24"}}
                         """))
        .build();

    final var res0 =
      this.client.send(req0, ofByteArray());

    final var errors =
      (LLv1Errors) this.messages.deserialize(res0.body());
    assertEquals(400, res0.statusCode());
    assertEquals("unexpected-message", errors.errors().get(0).errorCode());
  }
}
