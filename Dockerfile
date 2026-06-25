# Build stage
FROM gradle:8.10-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar -x generateJooq -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE 4567
ENTRYPOINT ["java", "-jar", "app.jar"]