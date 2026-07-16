# Build context is the repo root (see docker-compose.yml) - this service
# depends on both shared library jars, which must be installed to the
# local Maven repo before this module's own build can resolve them (no
# parent/reactor pom ties the modules together). These two COPY+RUN layers
# are byte-identical across every service's Dockerfile, so Docker's build
# cache reuses them after the first service builds - the libraries aren't
# actually reinstalled 7 times over.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY common-library ./common-library
RUN mvn -q -B -f common-library/pom.xml install -DskipTests

COPY sirp-security ./sirp-security
RUN mvn -q -B -f sirp-security/pom.xml install -DskipTests

COPY auth-service ./auth-service
RUN mvn -q -B -f auth-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /workspace/auth-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
