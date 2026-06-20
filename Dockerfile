FROM maven:3.9.9 AS build
ARG MODULE_NAME
WORKDIR /workspace
COPY . /workspace
RUN mvn -pl ${MODULE_NAME} -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE_NAME
COPY --from=build /workspace/${MODULE_NAME}/target/${MODULE_NAME}-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
