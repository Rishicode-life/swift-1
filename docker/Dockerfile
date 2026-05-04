# syntax=docker/dockerfile:1
ARG SERVICE=transaction-gateway
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY swiftpay-common swiftpay-common
COPY transaction-gateway transaction-gateway
COPY ledger-service ledger-service
COPY analytics-worker analytics-worker
ARG SERVICE
RUN mvn -q -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ARG SERVICE
COPY --from=build /build/${SERVICE}/target/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
