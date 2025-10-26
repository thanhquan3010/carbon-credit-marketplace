# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

# Copy parent pom
COPY ../pom.xml /app/parent-pom.xml

# Copy module files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/vehicle-service-*.jar app.jar

# Create non-root user
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8082/api/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
