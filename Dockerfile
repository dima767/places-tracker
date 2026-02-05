# Multi-stage build for Places Tracker Spring Boot application
# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon || return 0

# Copy source code
COPY src src

# Build the application (skip tests for faster builds)
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime image (use JDK for keytool to generate SSL certs)
FROM eclipse-temurin:25-jdk

WORKDIR /app

# Create non-root user for security
RUN groupadd -r placestracker && useradd -r -g placestracker placestracker

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy the entrypoint script
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Create directories for logs and certs
RUN mkdir -p /app/logs /app/certs && chown -R placestracker:placestracker /app

# Switch to non-root user
USER placestracker

# Expose both HTTP and HTTPS ports
EXPOSE 8080 8443

# Use entrypoint to generate certs if needed
ENTRYPOINT ["/app/docker-entrypoint.sh"]
