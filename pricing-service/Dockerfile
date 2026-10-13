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
# Pricing Service Maven project
# =========================
COPY pricing-service/.mvn/ pricing-service/.mvn/
COPY pricing-service/mvnw pricing-service/
COPY pricing-service/pom.xml pricing-service/

RUN chmod +x pricing-service/mvnw

# Cache Pricing Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./pricing-service/mvnw \
    -f pricing-service/pom.xml \
    dependency:go-offline -B

# Copy Pricing Service source
COPY pricing-service/src/ pricing-service/src/

# Build Pricing Service
RUN --mount=type=cache,target=/root/.m2 \
    ./pricing-service/mvnw \
    -f pricing-service/pom.xml \
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

COPY --from=build /workspace/pricing-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "/app/app.jar"]