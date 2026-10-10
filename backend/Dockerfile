# Multi-stage build: Maven build → JRE runtime
# Build stage: full Maven + JDK 17
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests package

# Runtime stage: JRE 17 only (no Maven)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/salon-management-system-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
