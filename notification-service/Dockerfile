# Build context is the repo root (see docker-compose.yml) - see
# auth-service/Dockerfile for why every service installs the two shared
# library jars first, and why that's cheap after the first build.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY common-library ./common-library
RUN mvn -q -B -f common-library/pom.xml install -DskipTests

COPY sirp-security ./sirp-security
RUN mvn -q -B -f sirp-security/pom.xml install -DskipTests

COPY notification-service ./notification-service
RUN mvn -q -B -f notification-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/notification-service/target/*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
