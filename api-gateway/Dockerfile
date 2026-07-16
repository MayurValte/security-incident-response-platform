# Build context is the repo root (see docker-compose.yml). api-gateway is a
# reactive service with no domain DTOs to share, so it depends on
# sirp-security only - not common-library, unlike the 7 domain services.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY sirp-security ./sirp-security
RUN mvn -q -B -f sirp-security/pom.xml install -DskipTests

COPY api-gateway ./api-gateway
RUN mvn -q -B -f api-gateway/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
