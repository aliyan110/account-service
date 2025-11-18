FROM maven:3.9.6-amazoncorretto-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

FROM amazoncorretto:17
WORKDIR /app
COPY --from=build /workspace/target/quarkus-app/quarkus-run.jar app.jar

EXPOSE 8080
ENV PORT=8080
CMD ["java", "-jar", "app.jar"]
