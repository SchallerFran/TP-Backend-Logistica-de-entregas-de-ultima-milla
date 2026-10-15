# Contexto de build: la RAIZ del repo (ver docker-compose.yml).
# Multi-stage: primero se instala logistica-commons en el .m2 de la imagen de
# build, despues se empaqueta el servicio. Asi "docker compose up --build"
# funciona en una maquina limpia sin pasos manuales previos.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY logistica-commons/pom.xml logistica-commons/pom.xml
COPY logistica-commons/src logistica-commons/src
RUN mvn -B -f logistica-commons/pom.xml clean install -DskipTests

COPY despacho-service/pom.xml despacho-service/pom.xml
COPY despacho-service/src despacho-service/src
RUN mvn -B -f despacho-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/despacho-service/target/despacho-service-1.0.0.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
