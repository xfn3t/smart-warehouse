FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app/backend/warehouse-service

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -T 1C

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app/backend/warehouse-service

COPY --from=build /app/backend/warehouse-service/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]