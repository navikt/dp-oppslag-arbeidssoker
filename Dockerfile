FROM navikt/java:12

COPY init-scripts/* /init-scripts/
COPY build/libs/*-all.jar app.jar

ENV LOG_FORMAT="logstash" \
    LOG_LEVEL="error"