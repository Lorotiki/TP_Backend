# Docker Compose Setup - TPI Backend 2026

Este proyecto utiliza un **Dockerfile genérico** y un **docker-compose.yml** centralizado para orquestar todos los microservicios.

## Estructura

- **Dockerfile** (raíz): Dockerfile multi-etapa genérico que construye cualquier módulo Maven
- **docker-compose.yml** (raíz): Define todos los servicios (PostgreSQL, Keycloak, microservicios, API Gateway)
- **Aplicaciones.yml actualizadas**: Configuradas para funcionar tanto en localhost como en Docker

## Servicios

| Servicio | Puerto | Base de Datos | Dependencias |
|----------|--------|---------------|--------------|
| PostgreSQL | 5432 | N/A | N/A |
| Keycloak | 8180 | PostgreSQL | postgres |
| Market Data Service | 8081 | N/A | - |
| Portfolio Service | 8082 | PostgreSQL | postgres, keycloak |
| Orders Service | 8083 | PostgreSQL | postgres, keycloak |
| History Service | 8084 | PostgreSQL | postgres, keycloak |
| API Gateway | 8085 | N/A | keycloak + todos los servicios |

## Configuración de Base de Datos

Todos los servicios de datos usan ahora una **base de datos única**: `stockmarket` en PostgreSQL con credenciales:
- **Usuario**: admin
- **Contraseña**: admin
- **Base de datos**: stockmarket

**Nota**: Los esquemas (`portfolio`, `orders`, `history`) se crean automáticamente mediante las migraciones de Hibernate (ddl-auto: none).

## Cómo Usar

### 1. Compilar todo el proyecto (opcional)
```bash
mvn clean install
```

### 2. Levantar los servicios
```bash
docker-compose up -d --build
```

Esto compilará cada servicio desde su módulo Maven y levantará los contenedores.

### 3. Ver logs
```bash
docker-compose logs -f
```

Para un servicio específico:
```bash
docker-compose logs -f api-gateway
docker-compose logs -f orders-service
```

### 4. Detener los servicios
```bash
docker-compose down
```

Para eliminar también los volúmenes (base de datos):
```bash
docker-compose down -v
```

## Acceso a Servicios

- **API Gateway**: http://localhost:8085
- **Market Data**: http://localhost:8081
- **Portfolio**: http://localhost:8082
- **Orders**: http://localhost:8083
- **History**: http://localhost:8084
- **Keycloak Admin**: http://localhost:8180 (usuario: admin, contraseña: admin)
- **PostgreSQL**: localhost:5433

> Si algún contenedor no levanta o no podés entrar desde `localhost`, revisá primero que no haya otro proceso usando `8081`, `8085` o `8180` en tu máquina.

```bash
ss -ltnp | grep -E ':8080|:8081|:8082|:8083|:8084|:8085|:8180' || true
```

## Variables de Entorno

Puedes sobrescribir las variables de entorno modificando el `docker-compose.yml`:

### API Gateway
- `KEYCLOAK_ISSUER_URI`: URL del issuer de Keycloak (default: http://keycloak:8080/realms/tpi)
- `MARKET_DATA_URI`: URL del Market Data Service (default: http://market-data-service:8081)
- `PORTFOLIO_URI`: URL del Portfolio Service (default: http://portfolio-service:8082)
- `ORDERS_URI`: URL del Orders Service (default: http://orders-service:8083)
- `HISTORY_URI`: URL del History Service (default: http://history-service:8084)

### Servicios de Datos
- `SPRING_DATASOURCE_URL`: URL de PostgreSQL (default: jdbc:postgresql://postgres:5432/stockmarket)
- `SPRING_DATASOURCE_USERNAME`: Usuario (default: admin)
- `SPRING_DATASOURCE_PASSWORD`: Contraseña (default: admin)
- `KEYCLOAK_URL`: URL de Keycloak (default: http://keycloak:8080)
- `SERVER_PORT`: Puerto del servicio (8081, 8082, 8083, 8084 según el servicio)

## Dockerfile Genérico

El Dockerfile acepta un argumento `MODULE_NAME` para construir diferentes módulos:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-25 AS build
ARG MODULE_NAME
WORKDIR /workspace
COPY . /workspace
RUN mvn -pl ${MODULE_NAME} -am clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
ARG MODULE_NAME
COPY --from=build /workspace/${MODULE_NAME}/target/${MODULE_NAME}-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

El docker-compose.yml pasa el argumento en la sección `build.args`.

## Notas Importantes

1. **Red Docker**: Los servicios están en la red `tpi-network` para comunicación interna
2. **Reintentos**: Los servicios tienen `restart: on-failure` para tolerancia a fallos
3. **Base de Datos Persistente**: El volumen `postgres_data` persiste los datos entre ejecuciones
4. **Java 25**: Todos los servicios usan Java 25 (eclipse-temurin:25-jre)
5. **Compilación en Build**: Cada contenedor compila su módulo durante el build, no requiere JAR pre-compilado

## Troubleshooting

### Los servicios no se conectan entre sí
- Verifica que todos estén en la misma red: `docker network inspect tpi_tpi-network`
- Usa los nombres de servicio del docker-compose (`postgres`, `keycloak`, `portfolio-service`, etc.)

### Puerto ya en uso
- Cambia los puertos en el `docker-compose.yml` en la sección `ports:`
- O detén los contenedores previos: `docker-compose down`

### Error al detener o recrear un contenedor
Si aparece algo como:

- `cannot stop container ... permission denied`
- `container with given ID already exists`

hacelo en este orden:

```bash
sudo systemctl restart snap.docker.dockerd
docker compose down --remove-orphans || true
docker compose up -d --build
```

Si PostgreSQL ya estaba usando `5432` en tu máquina, este compose lo publica en `5433` para evitar ese choque.

### Base de datos no se inicializa
- Verifica los logs: `docker-compose logs postgres`
- Asegúrate de que PostgreSQL esté corriendo antes que los servicios (order de `depends_on`)

### Fallo en compilación Maven
- Verifica que el código sea compilable: `mvn clean compile`
- Revisa los logs del build: `docker-compose logs market-data-service`
