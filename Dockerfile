FROM amazoncorretto:17-alpine
WORKDIR /app

# deploy.sh builds the executable JAR before invoking docker build.
COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
