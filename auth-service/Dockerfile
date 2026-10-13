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
# Auth Service Maven project
# =========================
COPY auth-service/.mvn/ auth-service/.mvn/
COPY auth-service/mvnw auth-service/
COPY auth-service/pom.xml auth-service/

RUN chmod +x auth-service/mvnw

# Cache Auth Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./auth-service/mvnw \
    -f auth-service/pom.xml \
    dependency:go-offline -B

# Copy Auth Service source
COPY auth-service/src/ auth-service/src/

# Build Auth Service
RUN --mount=type=cache,target=/root/.m2 \
    ./auth-service/mvnw \
    -f auth-service/pom.xml \
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

COPY --from=build /workspace/auth-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]