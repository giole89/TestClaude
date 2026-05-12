# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copia solo il pom prima per sfruttare la cache dei layer
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S finai && adduser -S finai -G finai
COPY --from=build /app/target/finai-backend-1.0.0.jar app.jar
RUN chown finai:finai app.jar

USER finai

EXPOSE 3001

ENTRYPOINT ["java", "-jar", "app.jar"]
