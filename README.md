# Tp_Backned_2026

Repositorio del TPI de Backend orientado a microservicios para mercado de acciones.

## Qué incluye hoy
- `tpi-platform`: infraestructura local, `docker-compose.yml`, variables de entorno, bases PostgreSQL por servicio y comandos de orquestación.
- `tpi-keycloak-config`: realm base de Keycloak con roles `USER` y `ADMIN`.
- `tpi-api-gateway`: gateway con routing y validación JWT para rutas privadas.
- `tpi-market-data-service`: cotizaciones públicas con catálogo estable de símbolos.
- `tpi-portfolio-service`: portfolio, saldo, depósitos y aplicación interna de trades.
- `tpi-orders-service`: alta de órdenes, matching síncrono y coordinación con portfolio/historial.
- `tpi-history-service`: auditoría e historial por usuario/global.
- `PLAN_TRABAJO_MICROSERVICIOS.md`: plan de trabajo, fases y contratos del sistema.
- `postman_collection_tpi_backend_2026.json`: colección de demo para la exposición.

## Estado actual
- Fase 1: completa.
- Fase 2: completa.
- Fase 3: completa.
- Fase 4: completa.
- Fase 5: completa.

## Estructura del repo
```text
tp_backned_2026/
├── PLAN_TRABAJO_MICROSERVICIOS.md
├── README.md
├── pom.xml
├── postman_collection_tpi_backend_2026.json
├── tpi-api-gateway/
├── tpi-history-service/
├── tpi-keycloak-config/
├── tpi-market-data-service/
├── tpi-orders-service/
├── tpi-platform/
└── tpi-portfolio-service/
```

## Servicios y puertos
- `tpi-market-data-service`: `8081`
- `tpi-portfolio-service`: `8082`
- `tpi-orders-service`: `8083`
- `tpi-history-service`: `8084`
- `tpi-api-gateway`: `8085`
- `keycloak`: `8080`

## Arranque rápido de la infraestructura
Requisitos:
- Docker
- Docker Compose plugin (`docker compose`)
- Java 21
- Maven 3.9+

Pasos:
```bash
cd /home/leandro/Documentos/UTN/Backend/tp_backned_2026/tpi-platform
cp .env.example .env
make test
make up-build
make ps
```

### Ver logs
```bash
make logs
```

### Bajar servicios
```bash
make down
```

### Reiniciar limpiando volúmenes
```bash
make reset
```

## Ejecución local sin Docker
Desde la raíz del repo:
```bash
mvn test
```

Levantar infraestructura y luego servicios por separado si querés depurar:
```bash
cd /home/leandro/Documentos/UTN/Backend/tp_backned_2026/tpi-platform
cp .env.example .env
make up

cd /home/leandro/Documentos/UTN/Backend/tp_backned_2026
mvn -pl tpi-market-data-service spring-boot:run
mvn -pl tpi-portfolio-service spring-boot:run
mvn -pl tpi-history-service spring-boot:run
mvn -pl tpi-orders-service spring-boot:run
mvn -pl tpi-api-gateway spring-boot:run
```

## Acceso a Keycloak
- URL: `http://localhost:8080`
- Admin: `admin`
- Password: `admin`
- Realm: `tpi`
- Roles: `USER`, `ADMIN`

## Endpoints principales
Público:
- `GET /quotes/{symbol}`

Privados:
- `GET /users/{userId}/portfolio`
- `POST /users/{userId}/deposits`
- `POST /orders/sell`
- `POST /orders/buy`
- `GET /users/{userId}/orders`
- `GET /users/{userId}/history`
- `GET /admin/history`

## Reglas de negocio implementadas
- La cotización es pública y estable por símbolo.
- El portfolio crea la cuenta on-demand si el usuario todavía no existe.
- Los depósitos impactan el saldo en ARS.
- Las órdenes de venta validan tenencias disponibles.
- Las órdenes de compra validan saldo y hacen matching síncrono con ventas abiertas compatibles.
- Si no hay oferta compatible, la orden de compra se rechaza de inmediato.
- Cada resultado de orden queda auditado en historial.

## Demo sugerida
1. Obtener un token de Keycloak para un usuario con rol `USER`.
2. Consultar `GET /quotes/NVDA` desde el gateway.
3. Consultar el portfolio de `user-demo-1`.
4. Registrar una venta para `user-demo-2`.
5. Ejecutar una compra para `user-demo-1`.
6. Consultar historial por usuario.
7. Consultar historial global con un token `ADMIN`.

La colección `postman_collection_tpi_backend_2026.json` ya trae esos requests.

## Notas
- Los scripts SQL de `tpi-platform/infra/db/*/init` se ejecutan al crear los volúmenes por primera vez.
- Si modificás los scripts, reiniciá con `make reset` y luego `make up`.
- Las pruebas automáticas hoy validan cotizaciones, portfolio, historial y carga de contexto del gateway/orders.
