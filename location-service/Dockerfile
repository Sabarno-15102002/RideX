# syntax=docker/dockerfile:1

# =========================
# Build stage
# =========================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# Cache Maven dependencies
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw clean package -DskipTests -B


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

COPY --from=build /workspace/target/*.jar app.jar

RUN chown -R ridex:ridex /app

USER ridex

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/app.jar"]