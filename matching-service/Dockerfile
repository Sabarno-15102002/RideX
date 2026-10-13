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
# Matching Service Maven project
# =========================
COPY matching-service/.mvn/ matching-service/.mvn/
COPY matching-service/mvnw matching-service/
COPY matching-service/pom.xml matching-service/

RUN chmod +x matching-service/mvnw

# Cache Matching Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./matching-service/mvnw \
    -f matching-service/pom.xml \
    dependency:go-offline -B

# Copy Matching Service source
COPY matching-service/src/ matching-service/src/

# Build Matching Service
RUN --mount=type=cache,target=/root/.m2 \
    ./matching-service/mvnw \
    -f matching-service/pom.xml \
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

COPY --from=build /workspace/matching-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "/app/app.jar"]