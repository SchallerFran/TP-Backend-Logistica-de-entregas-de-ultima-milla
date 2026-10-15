# Contexto de build: la RAIZ del repo (ver docker-compose.yml).
# Multi-stage: primero se instala logistica-commons en el .m2 de la imagen de
# build, despues se empaqueta el servicio. Asi "docker compose up --build"
# funciona en una maquina limpia sin pasos manuales previos.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY logistica-commons/pom.xml logistica-commons/pom.xml
COPY logistica-commons/src logistica-commons/src
RUN mvn -B -f logistica-commons/pom.xml clean install -DskipTests

COPY pedidos-service/pom.xml pedidos-service/pom.xml
COPY pedidos-service/src pedidos-service/src
RUN mvn -B -f pedidos-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/pedidos-service/target/pedidos-service-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
