# syntax=docker/dockerfile:1

# ============================================================================
#  Stage 1 — Build (Maven + JDK 21)
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies are resolved before the sources are copied, so this layer is
# reused on every rebuild in which pom.xml did not change.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# Split the fat jar into Spring Boot layers (dependencies change far less often
# than application code, so Docker can cache them separately).
RUN java -Djarmode=tools -jar target/ai-proxy-platform.jar \
        extract --layers --launcher --destination extracted

# ============================================================================
#  Stage 2 — Runtime (JRE only, no Maven, no JDK)
# ============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as an unprivileged user.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Most stable layer first, most volatile last.
COPY --from=build --chown=spring:spring /app/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /app/extracted/application/ ./

# Render injects PORT (10000 by default); application.properties already reads it.
ENV PORT=8080
EXPOSE 8080

# Tuned for Render's free instance (512 MB RAM, 0.1 CPU):
#  - RAMPercentage      : the JVM sizes the heap from the container limit, not the host.
#  - UseSerialGC        : lowest GC overhead on a single, fractional CPU.
#  - TieredStopAtLevel=1: faster cold start after Render spins the service down.
#  - ExitOnOutOfMemoryError: fail fast so Render restarts the instance.
# Override anything at runtime with the JAVA_TOOL_OPTIONS environment variable.
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+UseSerialGC", \
    "-XX:TieredStopAtLevel=1", \
    "-XX:+ExitOnOutOfMemoryError", \
    "org.springframework.boot.loader.launch.JarLauncher"]
