FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/account-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV QUARKUS_HTTP_PORT=${PORT:-8080}
CMD ["java", "-jar", "app.jar"]
