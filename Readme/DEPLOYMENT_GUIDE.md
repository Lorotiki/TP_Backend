# 🚀 Deployment y Testing - Base de Datos Unificada

## Paso 1: Limpiar Ambiente Anterior

```bash
# Detener todos los contenedores
docker compose down -v

# Eliminar volúmenes antiguos (si hay datos previos)
docker volume prune

# Limpiar imágenes (opcional)
docker image prune -f
```

## Paso 2: Compilar Proyecto

```bash
# Desde la raíz del proyecto
mvn clean compile

# O compilar y empaquetar
mvn clean package
```

## Paso 3: Levantar Docker Compose

```bash
# Construir imágenes y levantar servicios
docker compose up -d --build

# Ver el progreso
docker compose logs -f

# Esperar a que PostgreSQL esté listo (healthcheck)
# Debería ver: "PostgreSQL init process complete; ready for start up"
```

## Paso 4: Verificar que todo está UP

```bash
# Listar contenedores
docker compose ps

# Todos deberían estar en estado "Up"
```

## Paso 5: Conectarse a la Base de Datos

```bash
# Acceder a PostgreSQL
docker exec -it postgres psql -U admin -d stockmarket

# Dentro de psql:
\dt                              # Listar todas las tablas
SELECT * FROM accounts;          # Ver cuentas
SELECT * FROM positions;         # Ver posiciones
SELECT * FROM orders;            # Ver órdenes
SELECT * FROM order_fills;       # Ver ejecuciones
SELECT * FROM history_events;    # Ver eventos

\q                               # Salir
```

## Paso 6: Testear Endpoints

### Health Checks

```bash
# API Gateway
curl http://localhost:8085/actuator/health

# Market Data
curl http://localhost:8081/actuator/health

# Portfolio
curl http://localhost:8082/actuator/health

# Orders
curl http://localhost:8083/actuator/health

# History
curl http://localhost:8084/actuator/health
```

### Test Market Data

```bash
curl http://localhost:8085/quotes/AAPL
```

### Test Portfolio (crear cuenta)

```powershell
# PowerShell
$body = @{
    userId = "user123"
    balanceArs = 10000
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8082/users/user123/portfolio" `
  -Method Get
```

### Test Orders

```powershell
# Crear una orden de compra
$orderBody = @{
    userId = "user123"
    symbol = "AAPL"
    quantity = 10
    limitPrice = 150.50
    orderType = "BUY"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8083/orders" `
  -Method Post `
  -Headers @{"Content-Type"="application/json"} `
  -Body $orderBody
```

## Paso 7: Validación de Integridad

### Verificar Tablas Creadas

```sql
-- Conectarse a PostgreSQL
docker exec -it postgres psql -U admin -d stockmarket

-- Listar información de las tablas
\d accounts
\d positions
\d cash_movements
\d orders
\d order_fills
\d history_events
\d user_operation_view

-- Ver todas las relaciones
\d+
```

### Verificar Índices

```sql
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY tablename;
```

### Verificar Foreign Keys

```sql
SELECT
    constraint_name,
    table_name,
    column_name,
    foreign_table_name,
    foreign_column_name
FROM information_schema.key_column_usage
WHERE table_schema = 'public' AND foreign_table_name IS NOT NULL;
```

## Paso 8: Logs para Debugging

### Ver todos los logs
```bash
docker compose logs -f
```

### Ver logs de un servicio específico
```bash
docker compose logs -f api-gateway
docker compose logs -f portfolio-service
docker compose logs -f orders-service
docker compose logs -f history-service
docker compose logs -f postgres
docker compose logs -f keycloak
```

### Ver solo los últimos N líneas
```bash
docker compose logs --tail=100 -f api-gateway
```

## Paso 9: Testing de Migraciones (Hibernate)

Los logs de Hibernate mostrarán si todo está bien:

```
# Buscar en los logs de los servicios:
docker compose logs portfolio-service | grep -i "hibernate"

# Deberías ver algo como:
# "Hibernate: select ... from accounts"
```

## Paso 10: Agregar Datos de Prueba (Opcional)

Crear `/db-init/02_seed_data.sql`:

```sql
-- Insertar usuario de prueba
INSERT INTO accounts (id, user_id, balance_ars, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000'::uuid,
    'testuser@example.com',
    10000.00,
    NOW(),
    NOW()
) ON CONFLICT (user_id) DO NOTHING;

-- Reinicializar secuencias si es necesario
COMMIT;
```

Luego agregar a `docker-compose.yml`:

```yaml
volumes:
  - ./db-init/01_schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
  - ./db-init/02_seed_data.sql:/docker-entrypoint-initdb.d/02_seed_data.sql:ro
```

## 🔍 Checklist de Validación

- [ ] `docker compose ps` muestra todos los servicios "Up"
- [ ] `docker compose logs postgres` muestra "ready for start up"
- [ ] `curl http://localhost:8085/actuator/health` retorna estado "UP"
- [ ] `psql` conecta y lista las tablas correctamente
- [ ] `SELECT * FROM accounts;` no retorna error
- [ ] Prueba un endpoint GET (como `/quotes/AAPL`)
- [ ] Prueba un endpoint POST (crear orden)
- [ ] Los logs no muestran errores de tabla no encontrada

## ⚠️ Problemas Comunes

### Error: "relation 'accounts' does not exist"
**Solución**: El script SQL no se ejecutó
```bash
docker compose down -v
docker compose up -d --build
docker compose logs postgres
```

### Error: "FATAL: password authentication failed"
**Solución**: Verifica credenciales en `docker-compose.yml` y `application.yml`

### Error: "Liquibase could not acquire change log lock"
**Solución**: No es aplicable (no usamos Liquibase)

### Error: "Connection refused" en los servicios
**Solución**: PostgreSQL aún no está listo
```bash
docker compose logs postgres
# Espera a ver "ready for start up"
```

### Error: Hibernate dice "table doesn't exist"
**Solución**: Verifica que `ddl-auto: validate` esté en `application.yml`

## 🔄 Reset Completo

Si algo falla y quieres empezar desde cero:

```bash
# Detener y eliminar todo
docker compose down -v
docker system prune -f

# Reconstruir
docker compose up -d --build

# Verificar
docker compose ps
docker compose logs postgres | tail -20
```

---

**Última actualización**: 2026-06-20
**Base de Datos**: stockmarket (PostgreSQL 15)
**Servicios**: 5 microservicios + PostgreSQL + Keycloak
