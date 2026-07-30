# syntax=docker/dockerfile:1

# ---------- build stage ----------
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

# Cache dependencies first (only re-runs when pom.xml changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

# Build the application (tests run in CI, not in the image build)
COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests && cp target/*.jar app.jar

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-jammy AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS=""

HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
