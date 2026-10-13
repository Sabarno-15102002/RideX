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
# Payment Service Maven project
# =========================
COPY payment-service/.mvn/ payment-service/.mvn/
COPY payment-service/mvnw payment-service/
COPY payment-service/pom.xml payment-service/

RUN chmod +x payment-service/mvnw

# Cache Payment Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./payment-service/mvnw \
    -f payment-service/pom.xml \
    dependency:go-offline -B

# Copy Payment Service source
COPY payment-service/src/ payment-service/src/

# Build Payment Service
RUN --mount=type=cache,target=/root/.m2 \
    ./payment-service/mvnw \
    -f payment-service/pom.xml \
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

COPY --from=build /workspace/payment-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "/app/app.jar"]