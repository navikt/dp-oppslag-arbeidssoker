FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:7e845800a49b1d0902cf94033d14f819213ea7287b55e207eb02a61b5f464f2e

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.arbeidssoker.oppslag.AppKt"]