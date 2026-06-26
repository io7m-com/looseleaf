looseleaf
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.looseleaf/com.io7m.looseleaf.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.looseleaf%22)
[![Maven Central (snapshot)](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fcom%2Fio7m%2Flooseleaf%2Fcom.io7m.looseleaf%2Fmaven-metadata.xml&style=flat-square)](https://central.sonatype.com/repository/maven-snapshots/com/io7m/looseleaf/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/looseleaf.svg?style=flat-square)](https://codecov.io/gh/io7m-com/looseleaf)
![Java Version](https://img.shields.io/badge/21-java?label=java&color=e6c35c)

![com.io7m.looseleaf](./src/site/resources/looseleaf.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/looseleaf/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/looseleaf/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/looseleaf/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/looseleaf/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/looseleaf/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/looseleaf/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/looseleaf/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/looseleaf/actions?query=workflow%3Amain.windows.temurin.lts)|

## Repository Relocation

Development of this project has moved to an
[open-source but not open-contribution](https://sqlite.org/copyright.html#notopencontrib)
model.

Source code and commits will remain publicly available perpetually, but issues
and/or pull requests will be rejected and/or ignored. Additionally, this project
will now only be available via a read-only mirror at:

  https://codeberg.org/io7m-com/looseleaf


## looseleaf

The `looseleaf` package implements an HTTP-accessible key/value database with
ACID semantics and fine-grained role-based access control.

## Features

* [ACID](https://en.wikipedia.org/wiki/ACID) semantics.
* Atomic reads and updates for arbitrary sets of keys. An unlimited number of
  keys can be read, updated, and/or deleted in a single operation that is
  atomic with respect to all other database operations.
* Fine-grained role-based access control.
* A trivial HTTP interface for easy access from shell scripts.
* A strictly defined JSON protocol with a full schema.
* Convenient endpoints for use with command-line tools such as
  [curl](https://curl.se/).
* A small, easily auditable codebase with a heavy use of modularity for
  correctness.
* An extensive automated test suite with high coverage.
* A small footprint; the server is designed to run in tiny 16-32mb JVM heap
  configurations.
* Platform independence. No platform-dependent code is included in any form,
  and installations can largely be carried between platforms without changes.
  The database file format is also platform-independent.
* Security-conscious engineering. All requests require authentication,
  extensive validation is performed on all requests, and careful use is made of
  the Java type system to enforce invariants throughout the codebase.
* Fully instrumented with [OpenTelemetry](https://www.opentelemetry.io)
  for reliable service monitoring.
* Configurable fault injection for testing monitoring.
* [OSGi-ready](https://www.osgi.org/)
* [JPMS-ready](https://en.wikipedia.org/wiki/Java_Platform_Module_System)
* ISC license.

## Usage

See the [documentation](https://www.io7m.com/software/looseleaf).

