# syntax=docker/dockerfile:1

# =========================
# Build stage
# =========================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# =========================
# Shared RideX observability library
# =========================
COPY libs/ridex-observability/.mvn/ libs/ridex-observability/.mvn/
COPY libs/ridex-observability/mvnw libs/ridex-observability/
COPY libs/ridex-observability/pom.xml libs/ridex-observability/

RUN chmod +x libs/ridex-observability/mvnw

COPY libs/ridex-observability/src/ libs/ridex-observability/src/

RUN --mount=type=cache,target=/root/.m2 \
    ./libs/ridex-observability/mvnw \
    -f libs/ridex-observability/pom.xml \
    clean install -DskipTests -B

# =========================
# Location Service Maven project
# =========================
COPY location-service/.mvn/ location-service/.mvn/
COPY location-service/mvnw location-service/
COPY location-service/pom.xml location-service/

RUN chmod +x location-service/mvnw

# Cache Location Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./location-service/mvnw \
    -f location-service/pom.xml \
    dependency:go-offline -B

# Copy Location Service source
COPY location-service/src/ location-service/src/

# Build Location Service
RUN --mount=type=cache,target=/root/.m2 \
    ./location-service/mvnw \
    -f location-service/pom.xml \
    clean package -DskipTests -B

# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd \
    --system \
    --uid 1001 \
    --create-home \
    ridex

COPY --from=build /workspace/location-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/app.jar"]