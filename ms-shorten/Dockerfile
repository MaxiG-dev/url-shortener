# Start with a base image containing Java runtime and Maven
FROM maven:3.8.4-openjdk-17

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml file
COPY ./pom.xml ./pom.xml

# Build all the dependencies in preparation to go offline.
# This is a separate step so the dependencies will be cached unless
# the pom.xml file has changed.
RUN mvn dependency:go-offline -B

# Copy the project source
COPY src src

# Package the application
RUN mvn package -DskipTests

# Make port 8080 available to the world outside this container
EXPOSE 8080

# The application's jar file
#ARG JAR_FILE=target/ms-shorten-0.0.1-SNAPSHOT.jar

# Add the application's jar to the container
#ADD ./app/${JAR_FILE} ms-shorten.jar

# Run the jar file
#ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/ms-shorten.jar"]