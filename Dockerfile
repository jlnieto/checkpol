FROM node:22-bookworm-slim AS node

FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY --from=node /usr/local /usr/local

COPY package.json package-lock.json ./
RUN npm ci

COPY .mvn ./.mvn
COPY mvnw mvnw.cmd pom.xml ./
COPY src ./src

RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
