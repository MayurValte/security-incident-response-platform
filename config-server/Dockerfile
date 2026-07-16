# Build context is the repo root (see docker-compose.yml) - config-server
# has no dependency on common-library or sirp-security, unlike every other
# service, so this is the only Dockerfile that skips installing them.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY config-server ./config-server
RUN mvn -q -B -f config-server/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# curl backs docker-compose's healthcheck - every other service fail-fasts
# if config-server is unreachable at startup, so this is the one hard
# dependency gate worth healthchecking precisely.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/config-server/target/*.jar app.jar

EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
