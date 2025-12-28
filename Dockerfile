FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 80 5050
ENTRYPOINT ["java", "-jar", "app.jar"]
