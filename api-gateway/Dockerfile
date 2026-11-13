# Imagen del api-gateway. Se construye desde la RAÍZ del repo, igual que los otros servicios
# (ver docker-compose.yml):  docker build -f api-gateway/Dockerfile .

# ---- Etapa 1: compilar con Maven (no hace falta tener Maven ni JDK instalados en el host) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY api-gateway api-gateway
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f api-gateway/pom.xml -Dmaven.test.skip=true package

# ---- Etapa 2: imagen final, solo con el JRE y el JAR ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/api-gateway/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
