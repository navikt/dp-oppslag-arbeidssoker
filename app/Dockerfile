FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:c0427b77f66a72e57eaf3b73d304989dc2e3fb1791bc46ff217ffa59806d650c

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.arbeidssoker.oppslag.AppKt"]