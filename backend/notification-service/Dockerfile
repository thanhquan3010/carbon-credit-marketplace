FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Create a new stage for runtime
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=0 /app/target/*.jar app.jar

# Expose port
EXPOSE 8084

# Add health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8084/api/v1/notifications/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
