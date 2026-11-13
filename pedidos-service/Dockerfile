# Imagen de pedidos-service. Se construye desde la RAÍZ del repo (ver docker-compose.yml)
# porque necesita el JAR común:  docker build -f pedidos-service/Dockerfile .

# ---- Etapa 1: compilar con Maven (no hace falta tener Maven ni JDK instalados en el host) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero el JAR común: se instala en el repositorio local de Maven de esta etapa.
# El cache de ~/.m2 evita volver a bajar las dependencias en cada build; "locked" porque
# pedidos y despacho se construyen en paralelo y comparten ese cache.
COPY logistica-commons logistica-commons
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f logistica-commons/pom.xml -Dmaven.test.skip=true install

COPY pedidos-service pedidos-service
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f pedidos-service/pom.xml -Dmaven.test.skip=true package

# ---- Etapa 2: imagen final, solo con el JRE y el JAR ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/pedidos-service/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
