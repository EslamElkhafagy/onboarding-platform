# syntax=docker/dockerfile:1

# ---------- Stage 1: build the Angular SPA ----------
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
# Install deps first (cached unless lockfile changes)
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
# Build the production bundle -> dist/frontend/browser
COPY frontend/ ./
RUN npm run build

# ---------- Stage 2: build the Spring Boot jar (with SPA baked in) ----------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
# Resolve dependencies first (cached unless pom changes)
COPY pom.xml ./
RUN mvn -q -e -B dependency:go-offline
# Backend sources
COPY src ./src
# Copy the built SPA into Spring's static resources so it ships inside the jar
COPY --from=frontend /app/frontend/dist/frontend/browser/ ./src/main/resources/static/
RUN mvn -q -B clean package -DskipTests

# ---------- Stage 3: slim runtime ----------
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
# Uploaded files land here. NOTE: on free hosts this disk is ephemeral — original files are
# lost on redeploy/restart, but chunks+embeddings persist in Postgres so RAG keeps working.
RUN mkdir -p /app/data/uploads
COPY --from=backend /app/target/*.jar app.jar

# Keep the JVM inside a 512 MB free-tier instance.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
ENV STORAGE_DIR=/app/data/uploads
ENV SPRING_PROFILES_ACTIVE=prod

# PORT is provided by the host (Render/Koyeb); Spring reads ${PORT} (defaults to 8080).
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
