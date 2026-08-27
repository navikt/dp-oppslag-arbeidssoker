FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:48fbe93c21570def7e0a7f16f6a7d31509595d95196f60c06f2e7b9c24003277

ENV TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.arbeidssoker.oppslag.AppKt"]