# -------- Stage 1: Build the application --------
FROM maven:3.9.6-amazoncorretto-17 AS build
WORKDIR /workspace

# Copy only the pom.xml first to enable Docker layer caching
COPY pom.xml .

# Copy the src directory
COPY src src

# Build the application
RUN mvn package -DskipTests

# -------- Stage 2: Runtime image --------
FROM amazoncorretto:17
WORKDIR /app

# Copy jar built in Stage 1
COPY --from=build /workspace/target/account-service-1.0.0-SNAPSHOT.jar app.jar

# Railway will assign a dynamic port
ENV PORT=8080
EXPOSE 8080

# Start the service
CMD ["java", "-jar", "app.jar"]
