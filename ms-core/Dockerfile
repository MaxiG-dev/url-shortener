# Use the official Maven image with Java 17
FROM maven:3.8.3-openjdk-17 as builder

# Copy the project files to the container
COPY . /usr/src/app
WORKDIR /usr/src/app

# Build the application
RUN mvn clean package -DskipTests

# Use the official OpenJDK image for a smaller runtime image
FROM openjdk:17-slim

# Copy the built artifact from the builder stage
COPY --from=builder /usr/src/app/target/ms-core-0.0.1-SNAPSHOT.jar /usr/app/ms-core.jar

# Set the working directory
WORKDIR /usr/app

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
CMD ["java", "-jar", "ms-core.jar"]
