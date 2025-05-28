# Use an appropriate OpenJDK base image
FROM eclipse-temurin:17-jdk-jammy

# Set a working directory
WORKDIR /app

# Argument to specify the JAR file name (can be overridden at build time)
ARG JAR_FILE=build/libs/cultural-map-app-0.0.1-SNAPSHOT.jar

# Copy the built JAR file into the image
# The JAR_FILE path is relative to the build context (the root of the project)
COPY ${JAR_FILE} app.jar

# Expose the application port (default 8080, can be overridden by SPRING_PROFILES_ACTIVE or SERVER_PORT env var)
EXPOSE 8080

# Specify the command to run the application
# Entrypoint allows for passing arguments to the java command if needed, 
# while CMD specifies the default command to run.
ENTRYPOINT ["java", "-jar", "app.jar"]

# Optional: Add a healthcheck (requires spring-boot-starter-actuator dependency)
# HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#   CMD curl -f http://localhost:8080/actuator/health || exit 1
