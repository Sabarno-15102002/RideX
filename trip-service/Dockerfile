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
# Trip Service Maven project
# =========================
COPY trip-service/.mvn/ trip-service/.mvn/
COPY trip-service/mvnw trip-service/
COPY trip-service/pom.xml trip-service/

RUN chmod +x trip-service/mvnw

# Cache Trip Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./trip-service/mvnw \
    -f trip-service/pom.xml \
    dependency:go-offline -B

# Copy Trip Service source
COPY trip-service/src/ trip-service/src/

# Build Trip Service
RUN --mount=type=cache,target=/root/.m2 \
    ./trip-service/mvnw \
    -f trip-service/pom.xml \
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

COPY --from=build /workspace/trip-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]