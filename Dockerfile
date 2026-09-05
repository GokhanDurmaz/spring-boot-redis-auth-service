FROM gradle:8.5-jdk17 AS builder
WORKDIR /build

COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
COPY src src

RUN ./gradlew bootJar --no-daemon -x test

FROM amazoncorretto:17-alpine
WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]