# Stage 1: Build the application with Maven
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application, skipping tests for a faster build
RUN mvn clean install -DskipTests

# Stage 2: Create a lightweight final image
FROM openjdk:17-jre-slim

# Set the working directory
WORKDIR /app

# Copy the executable JAR from the build stage
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Set the command to run the application
CMD ["java", "-jar", "app.jar"]
