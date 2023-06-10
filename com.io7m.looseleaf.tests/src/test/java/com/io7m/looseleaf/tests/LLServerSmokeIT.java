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

import com.io7m.looseleaf.protocol.v1.LLv1Messages;
import com.io7m.looseleaf.protocol.v1.LLv1RUD;
import com.io7m.looseleaf.security.LLKeyName;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.io7m.looseleaf.server.api.LLServerAction.READ;
import static com.io7m.looseleaf.server.api.LLServerAction.WRITE;
import static java.net.http.HttpRequest.BodyPublishers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * An aggressive smoke test that blasts the server with many concurrent
 * transactions in an attempt to see how effective the retry behaviour is.
 */

public final class LLServerSmokeIT
{
  private static final Logger LOG =
    LoggerFactory.getLogger(LLServerSmokeIT.class);

  private static final int SMOKE_CONCURRENT_CLIENTS = 64;
  private static final int SMOKE_REQUESTS = SMOKE_CONCURRENT_CLIENTS * 1000;

  private Path directory;
  private LLPassword password0;
  private LLServers servers;
  private LLServerType server;
  private LLv1Messages messages;
  private List<String> words;
  private ExecutorService executor;

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
            "all-reader",
            List.of(new LLServerGrant(READ, "/*"))
          ),
          new LLServerRole(
            "all-writer",
            List.of(new LLServerGrant(WRITE, "/*"))
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
            List.of("all-reader", "all-writer")
          )
        ),
        Optional.empty(),
        Optional.empty()
      )
    );

    final var file =
      LLTestDirectories.resourceOf(
        LLServerSmokeIT.class,
        this.directory,
        "200-less-common.txt"
      );

    this.words =
      Files.lines(file, UTF_8)
        .toList();

    this.executor =
      Executors.newFixedThreadPool(SMOKE_CONCURRENT_CLIENTS);
  }

  @AfterEach
  public void tearDown()
    throws Exception
  {
    this.executor.shutdown();
    this.executor.awaitTermination(60L, TimeUnit.SECONDS);
    this.server.close();
    LLTestDirectories.deleteDirectory(this.directory);
  }

  @Test
  public void testSmoke()
    throws Exception
  {
    /*
     * The test is unreliable on windows due to thread contention.
     */

    assumeFalse(
      System.getProperty("os.name")
        .toUpperCase()
        .contains("WINDOWS")
    );

    /*
     * The test is expensive.
     */

    assumeTrue(
      System.getProperty("com.io7m.looseleaf.noExpensiveTests") == null
    );

    final var requests =
      IntStream.range(0, SMOKE_REQUESTS)
        .mapToObj(this::randomRUD)
        .map(this::doRequest)
        .toList()
        .toArray(new CompletableFuture[SMOKE_REQUESTS]);

    try {
      CompletableFuture.allOf(requests)
        .get();
    } catch (final Exception e) {
      // OK
    }

    var succeeded = 0;
    var failed = 0;

    for (final var future : requests) {
      try {
        future.get();
        ++succeeded;
      } catch (final Exception e) {
        ++failed;
      }
    }

    LOG.info("succeeded: {}", Integer.valueOf(succeeded));
    LOG.info("failed:    {}", Integer.valueOf(failed));

    final var failurePercentage =
      (double) failed / (double) SMOKE_REQUESTS;

    assertTrue(
      failurePercentage < 0.002,
      "Failure percentage %f must be < 0.002".formatted(failurePercentage)
    );
  }

  private static String base64(
    final String text)
  {
    return Base64.getUrlEncoder().encodeToString(text.getBytes(UTF_8));
  }

  private static String basic(
    final String user,
    final String pass)
  {
    return "Basic " + base64(user + ":" + pass);
  }

  private CompletableFuture<Object> doRequest(
    final LLv1RUD rud)
  {
    final var future = new CompletableFuture<>();
    this.executor.execute(() -> {
      try {
        final var client =
          HttpClient.newHttpClient();
        final var request =
          HttpRequest.newBuilder(URI.create("http://localhost:20000/v1/rud"))
            .header("Authorization", basic("grouch", "password0"))
            .POST(ofByteArray(this.messages.serialize(rud)))
            .build();

        final var response =
          client.send(request, ofString());

        LOG.debug("response: {}", response.body());

        if (response.statusCode() >= 400) {
          throw new IOException(String.valueOf(response.statusCode()));
        }

        future.complete(new Object());
      } catch (final Exception e) {
        LOG.error("error: ", e);
        future.completeExceptionally(e);
      }
    });
    return future;
  }

  private LLv1RUD randomRUD(
    final int index)
  {
    final var readCount =
      (int) (Math.random() * 8.0);
    final var updateCount =
      (int) (Math.random() * 8.0);
    final var deleteCount =
      (int) (Math.random() * 8.0);

    final Set<String> reads = new HashSet<>();
    for (int k = 0; k < readCount; ++k) {
      final var word = this.randomWord();
      reads.add(LLKeyName.create("/" + word).value());
    }

    final Map<String, String> updates = new HashMap<>();
    for (int k = 0; k < updateCount; ++k) {
      final var word = this.randomWord();
      updates.put(
        LLKeyName.create("/" + word).value(),
        Integer.toString(index)
      );
    }

    final Set<String> deletes = new HashSet<>();
    for (int k = 0; k < deleteCount; ++k) {
      final var word = this.randomWord();
      deletes.add(LLKeyName.create("/" + word).value());
    }

    return new LLv1RUD(reads, updates, deletes);
  }

  private String randomWord()
  {
    return this.words.get((int) (Math.random() * (double) this.words.size()));
  }
}
