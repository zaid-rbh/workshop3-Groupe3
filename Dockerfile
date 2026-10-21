# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy project files and build jar
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built artifact from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose application port
EXPOSE 8080

# Launch application (binding to 0.0.0.0 within the container)
ENTRYPOINT ["java", "-jar", "app.jar", "--server.address=0.0.0.0"]
