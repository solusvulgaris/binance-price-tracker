FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw \
    && ./mvnw -B clean package -DskipTests \
    && find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -exec cp {} /app/app.jar \;

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
