FROM ghcr.io/navikt/baseimages/temurin:19

COPY build/libs/*-all.jar app.jar

ENV LOG_FORMAT="logstash" \
    LOG_LEVEL="error"
