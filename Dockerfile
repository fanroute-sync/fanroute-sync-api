FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
