# STAGE 1: Build the Application
# Maven sobat Eclipse Temurin Java 17 vapruya (Stable)
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# STAGE 2: Run the Application
# OpenJDK chya aivaji Eclipse Temurin vapra (He Render var chaltay)
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]