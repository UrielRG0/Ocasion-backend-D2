# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jre-alpine AS runtime-base
RUN addgroup -S -g 10001 ocasion && adduser -S -D -H -u 10001 -G ocasion ocasion
WORKDIR /app
USER 10001:10001
EXPOSE 8081
ENV SERVER_PORT=8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8081/healthz || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# El pipeline descarga el JAR ya probado. La VM solo construye esta etapa.
FROM runtime-base AS ci-runtime
COPY --chown=10001:10001 .deploy-artifact/app.jar /app/app.jar

# Construcción manual desde el código: docker build -t ocasion-dom2:local .
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY salones/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline
COPY salones/src/main ./src/main
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

FROM runtime-base AS runtime
COPY --from=builder --chown=10001:10001 /build/target/*.jar /app/app.jar
