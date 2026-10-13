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
# Rider Service Maven project
# =========================
COPY rider-service/.mvn/ rider-service/.mvn/
COPY rider-service/mvnw rider-service/
COPY rider-service/pom.xml rider-service/

RUN chmod +x rider-service/mvnw

# Cache Rider Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./rider-service/mvnw \
    -f rider-service/pom.xml \
    dependency:go-offline -B

# Copy Rider Service source
COPY rider-service/src/ rider-service/src/

# Build Rider Service
RUN --mount=type=cache,target=/root/.m2 \
    ./rider-service/mvnw \
    -f rider-service/pom.xml \
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

COPY --from=build /workspace/rider-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]