# Build stage
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /workspace/app

# Copy only the necessary files for building
COPY pom.xml .
COPY src src/

# Build the application
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build /workspace/app/target/*.jar app.jar

# Expose the port the app runs on
EXPOSE 8080

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=production

# The command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
