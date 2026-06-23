# Build stage
FROM maven:3.9.6-eclipse-temurin-17@sha256:408d89a73e0dc1b14cfbc003f5729720855da4003a266ebf841cf2c298a8d144 AS build
WORKDIR /app

# Copy pom.xml first for dependency layer caching
COPY MyFirstClaude_project/passport-photo-backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY MyFirstClaude_project/passport-photo-backend/src/ src/
RUN mvn package -DskipTests -B

# Run stage — Ubuntu (glibc) required for OpenCV native libs; Alpine will not work
FROM eclipse-temurin:17-jre-jammy@sha256:d54695245a8c604aa3bfb0b6a2e2669a5e8d4e102d869ba5fb4712026b9983cf
WORKDIR /app

COPY --from=build /app/target/passport-photo-1.0.0.jar app.jar

EXPOSE 8080

# curl: health check; libgtk2.0-0: OpenCV highgui native lib requires GTK2 at runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl libgtk2.0-0 \
    && rm -rf /var/lib/apt/lists/*

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/api/countries || exit 1

# Run as non-root for security (port 8080 does not require root)
RUN addgroup --system appgroup \
    && adduser --system --no-create-home --shell /bin/false --ingroup appgroup appuser \
    && mkdir -p /app/native-cache \
    && chown appuser:appgroup /app/native-cache
USER appuser

# -Dorg.bytedeco.javacpp.cachedir: JavaCPP extracts OpenCV .so files here;
# avoids noexec /tmp failures in some container runtimes.
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:TieredStopAtLevel=1", \
    "-Dorg.bytedeco.javacpp.cachedir=/app/native-cache", \
    "-jar", "app.jar"]
