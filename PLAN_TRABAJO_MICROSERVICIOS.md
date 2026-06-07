# Plan de trabajo - Backend TPI Microservicios

> Documento reutilizable para guiar el desarrollo por etapas y registrar avances con commits incrementales.
> Reemplazar los placeholders `[[...]]` según el dominio del nuevo trabajo.

## 1. Objetivo
Construir un backend de mercado de acciones con autenticación OAuth2 + Keycloak, consulta pública de cotizaciones, gestión de portfolio/saldo, órdenes de compra/venta y consulta de historiales.

## 2. Alcance funcional extraído del enunciado
- Consultar cotizaciones por símbolo/código.
- Consultar portfolio y saldo de un usuario.
- Registrar y resolver órdenes de compra.
- Registrar y resolver órdenes de venta.
- Consultar historial completo de operaciones por usuario.
- Consultar historial total para un usuario ADMIN.

## 3. Arquitectura base propuesta
### Servicios mínimos
- `[[api-gateway]]`: único punto de entrada, routing y validación de tokens.
- `[[market-data-service]]`: consulta pública de cotizaciones y conversión de moneda.
- `[[portfolio-service]]`: saldo en ARS, tenencias y movimientos de cuenta.
- `[[orders-service]]`: creación y resolución síncrona de órdenes de compra/venta.
- `[[history-service]]`: historial completo por usuario y consulta global para `ADMIN`.
- `[[keycloak]]`: proveedor de identidad y autorización.

### Flujo principal de compra/venta
1. Cliente entra por `api-gateway`.
2. `api-gateway` valida token con Keycloak.
3. `orders-service` recibe la orden y consulta cotización vigente en `market-data-service`.
4. `orders-service` verifica compatibilidad de precio/cantidad con ofertas disponibles.
5. Si la operación es válida, actualiza saldo/tenencias en `portfolio-service`.
6. `history-service` registra el resultado final, aceptado o rechazado.

### Decisiones a fijar antes de programar
1. Persistencia por servicio, evitando base compartida salvo necesidad puntual.
2. Matching síncrono, porque el enunciado pide respuesta inmediata.
3. Conversión de moneda apoyada en API externa o tabla paramétrica mockeada.
4. Contratos entre servicios definidos primero para evitar acoplamiento accidental.

## 4. Contratos mínimos por servicio
### `[[api-gateway]]`
- Punto de entrada único para todos los clientes.
- Rutea llamadas públicas y privadas.
- Valida el token contra Keycloak antes de dejar pasar accesos protegidos.

### `[[market-data-service]]`
- `GET /quotes/{symbol}`: devuelve cotización, moneda, símbolo y timestamp.
- Respuesta mínima: `symbol`, `price`, `currency`, `source`, `updatedAt`.
- Este servicio alimenta el endpoint público de cotizaciones.

### `[[portfolio-service]]`
- `GET /users/{userId}/portfolio`: devuelve saldo y tenencias.
- `POST /users/{userId}/deposits`: registra ingresos en ARS.
- Respuesta mínima: `balanceArs`, `positions[]`, `movementId`.

### `[[orders-service]]`
- `POST /orders/buy`: crea y resuelve una orden de compra.
- `POST /orders/sell`: crea y registra una orden de venta.
- Payload mínimo: `userId`, `symbol`, `quantity`, `priceLimit`, `side`.
- Respuesta mínima: `orderId`, `status`, `matchedQuantity`, `remainingQuantity`.

### `[[history-service]]`
- `GET /users/{userId}/history`: historial completo de un usuario.
- `GET /admin/history`: historial global para `ADMIN`.
- `POST /events`: guarda cada operación resuelta o rechazada.

### `[[keycloak]]`
- Emite tokens OAuth2 y define roles `USER` y `ADMIN`.
- Las rutas privadas dependen de sus claims para autorización.

### Reglas de contrato que no conviene romper
- Cotizaciones públicas sin token, pero el resto de rutas privadas con autenticación.
- `orders-service` decide aceptación o rechazo en forma inmediata.
- `history-service` solo registra trazas; no debería calcular reglas de negocio.
- `portfolio-service` mantiene saldo y tenencias; `orders-service` orquesta la operación.

## 5. Plan de entrega por fases
### Fase 1 - Base del dominio y contratos
**Objetivo:** dejar definidos los casos de uso y la interfaz entre servicios.

**Tareas:**
- Modelar entidades y casos de uso mínimos del dominio.
- Definir contratos entre servicios y endpoints públicos.

**Criterio de salida:**
- Los servicios y sus responsabilidades están claros.
- Los endpoints principales ya están documentados.

**Avance esperado:** `1/5`

### Fase 2 - Seguridad e infraestructura
**Objetivo:** tener autenticación, autorización y entorno ejecutable.

**Tareas:**
- Implementar seguridad con Keycloak y roles `USER` / `ADMIN`.
- Montar Docker Compose con la base del sistema.

**Criterio de salida:**
- El acceso privado queda protegido.
- El proyecto levanta con infraestructura reproducible.

**Avance esperado:** `2/5`

### Fase 3 - Cotizaciones y portfolio
**Objetivo:** exponer cotizaciones y gestionar saldo/tenencias.

**Tareas:**
- Resolver consulta de cotizaciones con una API externa o datos mockeados.
- Implementar portfolio y saldo con operaciones en ARS.

**Criterio de salida:**
- Se puede consultar una cotización pública.
- Se puede ver el estado de cuenta de un usuario.

**Avance esperado:** `3/5`

### Fase 4 - Órdenes y matching
**Objetivo:** registrar órdenes y resolver compras/ventas con reglas de negocio.

**Tareas:**
- Implementar alta y validación de órdenes de venta.
- Implementar el motor de emparejamiento para órdenes de compra.

**Criterio de salida:**
- Una orden válida se acepta o rechaza en forma inmediata.
- Se respetan cantidad, precio y remanentes.

**Avance esperado:** `4/5`

### Fase 5 - Historial y demo final
**Objetivo:** dejar trazabilidad completa y preparar la exposición.

**Tareas:**
- Registrar cada operación en el historial.
- Preparar colección Postman para demostración final.

**Criterio de salida:**
- El usuario ve su historial y `ADMIN` ve el global.
- La demo reproduce los casos clave sin intervención manual.

**Avance esperado:** `5/5`

## 6. Commit incremental recomendado
Hacer commits chicos por fase completa, no por cada microtarea.

### Secuencia sugerida de commits
1. `chore: base del proyecto y contratos iniciales`
2. `feat: seguridad e infraestructura del entorno`
3. `feat: cotizaciones y portfolio`
4. `feat: orders y matching`
5. `feat: historial y preparación de demo`

### Regla práctica de trabajo
- Al cerrar una fase: validar, commitear y marcar el avance.
- Si una fase queda grande: dividirla antes de empezar la siguiente.
- Evitar mezclar cambios de infraestructura, negocio y documentación.

## 7. Reglas clave del matching
- No comprar si no existe oferta compatible.
- Respetar cantidad disponible y precio límite.
- Si una venta cubre parcialmente una compra, actualizar el remanente.
- Mantener trazabilidad de cada operación resuelta o rechazada.

## 8. Reglas de seguridad
- Todas las rutas privadas requieren autenticación con Keycloak.
- El endpoint de cotizaciones es público.
- Solo `ADMIN` puede consultar el historial global.

## 9. Entregables técnicos
- `docker-compose.yml` funcionando.
- Colección Postman exportable.
- Documentación mínima de endpoints y variables de entorno.
- Evidencia de casos de compra, venta, rechazo y consulta de historial.

## 10. Orden recomendado de trabajo
- Fase 1: dominio, decisiones y contratos.
- Fase 2: seguridad e infraestructura.
- Fase 3: cotizaciones y portfolio.
- Fase 4: órdenes y matching.
- Fase 5: historial, pruebas y demo.

## 11. Checklist de avance
- [x] Fase 1 completa: dominio y contratos.
- [x] Fase 2 completa: seguridad e infraestructura.
- [ ] Fase 3 completa: cotizaciones y portfolio.
- [ ] Fase 4 completa: órdenes y matching.
- [ ] Fase 5 completa: historial y demo.

### Indicador simple de progreso
- `2/5 fases completadas`
- `2/5 entregables principales listos`

## 12. Cómo reutilizar este archivo en otro proyecto
- Reemplazar `[[...]]` por nombres reales del dominio.
- Mantener la misma estructura de pasos y checklist.
- Ajustar la lista de microservicios según el contexto.
- Cambiar reglas de negocio, seguridad y entregables según el enunciado nuevo.

## 13. Mapa de implementación real (sugerido)
### Nombres concretos de repos/servicios
- `tpi-platform`: orquestación local, compose y documentación de arranque.
- `tpi-keycloak-config`: realm, clientes, usuarios de prueba y roles.
- `tpi-api-gateway`: punto de entrada único, routing y validación JWT.
- `tpi-market-data-service`: cotizaciones y conversión de moneda.
- `tpi-portfolio-service`: saldo en ARS, tenencias y depósitos.
- `tpi-orders-service`: creación y resolución síncrona de buy/sell.
- `tpi-history-service`: auditoría e historial por usuario/global.

### Orden exacto de creación recomendado
1. `tpi-platform`
2. `tpi-keycloak-config`
3. `tpi-api-gateway`
4. `tpi-market-data-service`
5. `tpi-portfolio-service`
6. `tpi-orders-service`
7. `tpi-history-service`

## 14. Entregable mínimo por servicio
- `tpi-platform`: `docker-compose.yml` funcional + `README` de levantado.
- `tpi-keycloak-config`: export de realm con roles `USER` y `ADMIN`.
- `tpi-api-gateway`: rutas públicas/privadas funcionando con validación de token.
- `tpi-market-data-service`: `GET /quotes/{symbol}` operativo con respuesta estable.
- `tpi-portfolio-service`: `GET /users/{userId}/portfolio` y `POST /users/{userId}/deposits`.
- `tpi-orders-service`: `POST /orders/buy` y `POST /orders/sell` con respuesta inmediata.
- `tpi-history-service`: `POST /events`, `GET /users/{userId}/history`, `GET /admin/history`.

## 15. Definition of Done por fase
### Fase 1 (1/5)
- Contratos de API definidos para todos los servicios.
- Responsabilidades por servicio documentadas y sin solapamientos.
- Reglas de matching y seguridad validadas por el equipo.

### Fase 2 (2/5)
- Keycloak operativo con roles y clientes configurados.
- Gateway validando JWT en rutas privadas.
- Entorno local levantando con compose sin intervención manual.

### Fase 3 (3/5)
- Cotización pública respondiendo con símbolo, moneda y timestamp.
- Portfolio devolviendo saldo/tenencias correctamente.
- Depósitos impactando el saldo en ARS.

### Fase 4 (4/5)
- Buy/sell aceptan o rechazan en forma inmediata.
- Matching respeta precio, cantidad y remanentes.
- Portfolio se actualiza de forma consistente tras la operación.

### Fase 5 (5/5)
- Cada operación genera evento de historial.
- Usuario consulta su historial completo.
- `ADMIN` consulta historial global.
- Colección Postman final lista para exposición.

## 16. Dependencias y puntos de integración
- `tpi-api-gateway` depende de Keycloak y enruta a todos los servicios.
- `tpi-orders-service` depende de `tpi-market-data-service`, `tpi-portfolio-service` y `tpi-history-service`.
- `tpi-history-service` no decide reglas de negocio; solo persiste eventos.
- `tpi-portfolio-service` no ejecuta matching; solo mantiene estado de cuenta.

## 17. Estrategia de base de datos
- Mantener una base por microservicio (`portfolio`, `orders`, `history`) para reducir acoplamiento.
- Evitar joins cross-service; la integración entre dominios se hace por API/eventos.
- Mantener `user_id` como referencia externa estable (emitida por identidad), no FK entre servicios.
- Usar migraciones versionadas por servicio para evolución controlada.

## 18. Ubicación del script iniciador de base
La inicialización queda en `tpi-platform`, dentro de infraestructura compartida.

### Estructura recomendada
- `tpi-platform/infra/docker-compose.yml`
- `tpi-platform/infra/db/portfolio/init/01_schema.sql`
- `tpi-platform/infra/db/portfolio/init/02_tables.sql`
- `tpi-platform/infra/db/portfolio/init/03_seed.sql`
- `tpi-platform/infra/db/orders/init/01_schema.sql`
- `tpi-platform/infra/db/orders/init/02_tables.sql`
- `tpi-platform/infra/db/orders/init/03_seed.sql`
- `tpi-platform/infra/db/history/init/01_schema.sql`
- `tpi-platform/infra/db/history/init/02_tables.sql`
- `tpi-platform/infra/db/history/init/03_seed.sql`

### Convención de carga
- `01_schema.sql`: creación de schema y extensiones necesarias.
- `02_tables.sql`: tablas, índices y constraints.
- `03_seed.sql`: datos iniciales para demo/pruebas.

## 19. Estructura mínima de BD para crear modelos
### `portfolio-service`
**Tabla `accounts`**
- `id` (uuid, pk)
- `user_id` (varchar, unique)
- `balance_ars` (numeric(18,2), not null)
- `created_at`, `updated_at` (timestamp)

**Tabla `positions`**
- `id` (uuid, pk)
- `account_id` (uuid)
- `symbol` (varchar)
- `quantity` (numeric(18,4), not null)
- `avg_price_ars` (numeric(18,4), not null)
- `updated_at` (timestamp)
- unique `(account_id, symbol)`

**Tabla `cash_movements`**
- `id` (uuid, pk)
- `account_id` (uuid)
- `type` (`DEPOSIT`, `BUY_DEBIT`, `SELL_CREDIT`)
- `amount_ars` (numeric(18,2), not null)
- `reference_id` (varchar)
- `created_at` (timestamp)

### `orders-service`
**Tabla `orders`**
- `id` (uuid, pk)
- `user_id` (varchar, not null)
- `symbol` (varchar, not null)
- `side` (`BUY`, `SELL`)
- `quantity` (numeric(18,4), not null)
- `remaining_quantity` (numeric(18,4), not null)
- `limit_price` (numeric(18,4), not null)
- `status` (`PENDING`, `PARTIALLY_FILLED`, `FILLED`, `REJECTED`, `CANCELLED`, `EXPIRED`)
- `created_at`, `updated_at` (timestamp)

**Tabla `order_fills`**
- `id` (uuid, pk)
- `buy_order_id` (uuid)
- `sell_order_id` (uuid)
- `symbol` (varchar)
- `quantity` (numeric(18,4), not null)
- `price_ars` (numeric(18,4), not null)
- `executed_at` (timestamp)

### `history-service`
**Tabla `history_events`**
- `event_id` (uuid, pk)
- `event_type` (varchar)
- `user_id` (varchar)
- `order_id` (uuid)
- `correlation_id` (uuid)
- `causation_id` (uuid)
- `payload_json` (jsonb)
- `occurred_at` (timestamp)

**Opcional: tabla `user_operation_view` (lectura rápida)**
- `id` (uuid, pk)
- `user_id` (varchar)
- `operation_type` (varchar)
- `symbol` (varchar)
- `amount_ars` (numeric(18,2))
- `created_at` (timestamp)

### Criterio de IDs y consistencia
- Usar UUID para entidades principales.
- Registrar `correlation_id` para trazar una operación completa entre servicios.
- Si hay reintentos/eventos, usar `idempotency_key` en escritura para evitar duplicados.

