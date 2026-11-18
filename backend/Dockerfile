# syntax=docker/dockerfile:1

# ---- Build stage: Maven wrapper on a full JDK 21 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Download dependencies first so this layer is cached until pom.xml changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests need Docker (Testcontainers), which isn't available inside this build; run them before deploying.
RUN ./mvnw -B -q package -DskipTests \
    && cp target/*.jar app.jar

# ---- Run stage: slim JRE 21 ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /workspace/app.jar app.jar
USER app

# Render's free tier has 512 MB RAM; keep the heap well below it (metaspace, threads and buffers need the rest).
ENV JAVA_OPTS="-Xmx300m"

# Render sets PORT; application.yml reads it (server.port=${PORT:8080}).
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
