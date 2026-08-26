FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --create-home --shell /bin/bash appuser

COPY --from=build /workspace/target/*.jar pcverse.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/pcverse.jar"]