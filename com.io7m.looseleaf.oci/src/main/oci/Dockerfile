ARG looseleaf_version

FROM docker.io/library/alpine:3.16.0
FROM docker.io/library/eclipse-temurin:17.0.3_7-jre-alpine

ENV PATH="/looseleaf/bin:/sbin:/bin:/usr/sbin:/usr/bin:/opt/java/openjdk/bin"
ENV LOOSELEAF_HOME="/looseleaf"

COPY looseleaf /looseleaf

LABEL "org.opencontainers.image.authors"="Mark Raynsford"
LABEL "org.opencontainers.image.description"="Minimalist transactional HTTP key/value store"
LABEL "org.opencontainers.image.licenses"="ISC"
LABEL "org.opencontainers.image.source"="https://www.github.com/io7m/looseleaf"
LABEL "org.opencontainers.image.title"="Looseleaf"
LABEL "org.opencontainers.image.url"="https://www.io7m.com/software/looseleaf"
LABEL "org.opencontainers.image.version"="$looseleaf_version"

VOLUME "/looseleaf/etc"

CMD ["/looseleaf/bin/looseleaf"]
