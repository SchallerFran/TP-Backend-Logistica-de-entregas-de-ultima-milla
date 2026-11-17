
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY logistica-commons logistica-commons
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f logistica-commons/pom.xml -Dmaven.test.skip=true install

COPY despacho-service despacho-service
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -q -f despacho-service/pom.xml -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /build/despacho-service/target/*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
