FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd -r -u 10001 platform
COPY --from=build /src/target/backend-0.1.0-SNAPSHOT.jar /app/app.jar
USER platform
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
