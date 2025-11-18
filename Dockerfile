# Stage 1: Build
FROM maven:3.9.6-amazoncorretto-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY src src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM amazoncorretto:17
WORKDIR /app

# Copy the entire quarkus-app folder
COPY --from=build /workspace/target/quarkus-app /app

EXPOSE 8080
ENV PORT=8080
ENV JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0"

# Run the application using quarkus-run.jar
CMD ["java", "-jar", "quarkus-run.jar"]
