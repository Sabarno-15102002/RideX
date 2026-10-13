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
# Driver Service Maven project
# =========================
COPY driver-service/.mvn/ driver-service/.mvn/
COPY driver-service/mvnw driver-service/
COPY driver-service/pom.xml driver-service/

RUN chmod +x driver-service/mvnw

# Cache Driver Service dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./driver-service/mvnw \
    -f driver-service/pom.xml \
    dependency:go-offline -B

# Copy Driver Service source
COPY driver-service/src/ driver-service/src/

# Build Driver Service
RUN --mount=type=cache,target=/root/.m2 \
    ./driver-service/mvnw \
    -f driver-service/pom.xml \
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

COPY --from=build /workspace/driver-service/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]