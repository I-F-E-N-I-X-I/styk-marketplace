FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn -B -ntp package -DskipTests
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S styk && adduser -S styk -G styk
WORKDIR /app
COPY --from=build /app/target/marketplace-1.0.0.jar app.jar
USER styk
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
