
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY logistica-commons logistica-commons
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f logistica-commons/pom.xml -Dmaven.test.skip=true install

COPY pedidos-service pedidos-service
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f pedidos-service/pom.xml -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/pedidos-service/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
