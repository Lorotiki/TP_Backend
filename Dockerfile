FROM maven:3.9.9 AS build
ARG MODULE_NAME
WORKDIR /workspace

# Copiar solo archivos pom.xml para aprovechar caché de dependencias
COPY pom.xml /workspace/
COPY ${MODULE_NAME}/pom.xml /workspace/${MODULE_NAME}/
RUN mvn -pl ${MODULE_NAME} -am dependency:resolve -q 2>/dev/null || true

# Copiar código fuente
COPY . /workspace

# Compilar
RUN mvn -pl ${MODULE_NAME} -am clean package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE_NAME
COPY --from=build /workspace/${MODULE_NAME}/target/${MODULE_NAME}-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
