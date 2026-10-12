FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:cce692670be1b14799eee9524e0815de4e3de9e1a374cd73628543f14a0dd61e

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.arbeidssoker.oppslag.AppKt"]