# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :fruit-machine-service:bootJar --no-daemon

# Run stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/fruit-machine-service/build/libs/fruit-machine-service.jar app.jar
RUN useradd --system --no-create-home fruitmachine
USER fruitmachine
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
