# Contexto de build: la RAIZ del repo (ver docker-compose.yml).
# El gateway es reactivo y NO depende de logistica-commons.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY api-gateway/pom.xml api-gateway/pom.xml
COPY api-gateway/src api-gateway/src
RUN mvn -B -f api-gateway/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/api-gateway/target/api-gateway-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
