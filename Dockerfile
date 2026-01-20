# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/demo-login-0.0.1-SNAPSHOT.jar demo-login.jar

EXPOSE 8080

ENV SERVER_PORT=8080

CMD ["sh", "-c", "java -jar demo-login.jar --server.port=${PORT:-8080}"]
