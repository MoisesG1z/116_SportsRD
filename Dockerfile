# Build stage using JDK 17
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy the Gradle files and build cache configuration
COPY gradle /app/gradle
COPY gradlew /app/gradlew
COPY gradle.properties /app/gradle.properties
COPY settings.gradle.kts /app/settings.gradle.kts
COPY build.gradle.kts /app/build.gradle.kts

# Copy the source folders of all subprojects
COPY shared /app/shared
COPY composeApp /app/composeApp
COPY server /app/server

# Give execute permission and run the build for the server module
RUN chmod +x gradlew
RUN ./gradlew :server:installDist --no-daemon

# Run stage using JRE 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built distribution from the build stage
COPY --from=build /app/server/build/install/server /app

# Expose the default Ktor port (Render overrides this with the PORT env var)
EXPOSE 8080

# Run the Ktor application
CMD ["./bin/server"]
