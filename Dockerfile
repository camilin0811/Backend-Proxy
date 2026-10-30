# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/ai-proxy-platform.jar app.jar
# Railway injects PORT; application.properties reads it (default 8080)
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
