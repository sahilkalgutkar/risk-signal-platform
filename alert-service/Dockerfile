FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common common
COPY transaction-api transaction-api
COPY risk-scoring-service risk-scoring-service
COPY alert-service alert-service
RUN mvn -q -pl alert-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/alert-service/target/alert-service-exec.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
