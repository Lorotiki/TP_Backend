# Migración a Base de Datos Unificada - TPI Backend 2026

## 📋 Resumen de Cambios

Se ha consolidado de una arquitectura de **múltiples esquemas en una sola base de datos** a una arquitectura de **base de datos única compartida** sin separación por esquemas.

### Antes (Multi-Schema)
```
stockmarket (BD única con múltiples esquemas)
├── Schema: portfolio
│   ├── accounts
│   ├── positions
│   └── cash_movements
├── Schema: orders
│   ├── orders
│   └── order_fills
└── Schema: history
    ├── history_events
    └── user_operation_view
```

### Ahora (Unificado)
```
stockmarket (BD única con todas las tablas en public schema)
├── accounts
├── positions
├── cash_movements
├── orders
├── order_fills
├── history_events
├── user_operation_view
└── audit_log (nuevo)
```

## 🔄 Cambios Realizados

### 1. Scripts SQL de Inicialización
- **Archivo**: `/db-init/01_schema.sql`
- **Contenido**:
  - Creación de todas las tablas en un único esquema (public)
  - Índices para optimización de queries
  - Relaciones de foreign keys
  - Comentarios de documentación
  - Tabla `audit_log` para auditoría (opcional)

### 2. Entidades JPA Actualizadas
Se removió `schema = "nombreSchema"` de todas las anotaciones `@Table`:

**Portfolio Service:**
- `Account.java`: `@Table(name = "accounts")` (antes: `schema = "portfolio"`)
- `Position.java`: `@Table(name = "positions")` (antes: `schema = "portfolio"`)
- `CashMovement.java`: `@Table(name = "cash_movements")` (antes: `schema = "portfolio"`)

**Orders Service:**
- `OrderEntity.java`: `@Table(name = "orders")` (antes: `schema = "orders"`)
- `OrderFill.java`: `@Table(name = "order_fills")` (antes: `schema = "orders"`)

**History Service:**
- `HistoryEvent.java`: `@Table(name = "history_events")` (antes: `schema = "history"`)
- `UserOperationView.java`: `@Table(name = "user_operation_view")` (antes: `schema = "history"`)

### 3. Configuración Hibernate (application.yml)
Actualizado en los 3 servicios de datos:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Antes: none
    properties:
      hibernate:
        # REMOVIDO: default_schema: nombreSchema
        jdbc:
          time_zone: UTC
```

**Cambios:**
- `ddl-auto: validate` → Hibernate verifica que las tablas existan (no crea ni modifica)
- Removido `default_schema` → Usa el esquema público por defecto
- El script SQL inicial (`01_schema.sql`) crea todo en la inicialización

### 4. Docker Compose Actualizado
- Agregado volumen para ejecutar automáticamente `/db-init/01_schema.sql` al iniciar PostgreSQL
- Agregado `healthcheck` a PostgreSQL para verificar conectividad
- PostgreSQL espera a completar la inicialización antes que Keycloak y otros servicios

```yaml
volumes:
  - ./db-init/01_schema.sql:/docker-entrypoint-initdb.d/01_schema.sql:ro
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U admin -d stockmarket"]
  interval: 10s
  timeout: 5s
  retries: 5
```

### 5. Eliminación de tpi-platform
Ya no es necesario mantener `/tpi-platform/infra/db` con scripts personalizados. Todo se centraliza en `/db-init/`.

## 🚀 Ventajas de esta Migración

1. **Simplificación**: Una sola base de datos, una sola conexión
2. **Facilidad de Gestión**: Scripts centralizados en `/db-init/`
3. **Consistencia**: Todas las tablas en el mismo esquema (public)
4. **Portabilidad**: Migrar/clonar la BD es más simple
5. **Performance**: Menos complejidad en conexiones y transacciones
6. **Auditoría**: Tabla `audit_log` para seguimiento de cambios
7. **Escalabilidad**: Fácil de replicas o sharding futuro

## 📊 Relaciones entre Tablas

```
accounts (PK: id)
  ├──> positions (FK: account_id)
  └──> cash_movements (FK: account_id)

orders (PK: id)
  └──> order_fills (FK: buy_order_id, sell_order_id)

history_events (PK: event_id)
  └── (referencia opcional a order_id)

user_operation_view (PK: id)
  └── referencia a user_id
```

## ⚠️ Consideraciones de Migración

### Si tienes datos existentes:

```sql
-- Backup de datos existentes
pg_dump -U admin stockmarket > backup_before_migration.sql

-- Copiar datos de esquemas antiguos (si los tienes)
INSERT INTO accounts SELECT * FROM portfolio.accounts;
INSERT INTO positions SELECT * FROM portfolio.positions;
INSERT INTO cash_movements SELECT * FROM portfolio.cash_movements;
-- ... etc para cada tabla

-- Verificar integridad
SELECT COUNT(*) FROM accounts;
SELECT COUNT(*) FROM orders;
-- ... etc
```

### Si es primera vez (sin datos existentes):
- El script `01_schema.sql` se ejecuta automáticamente al levantar Docker
- No requiere pasos adicionales

## 🔍 Verificación

Después de levantar los servicios, verifica:

```bash
# Conectarse a PostgreSQL
docker exec -it postgres psql -U admin -d stockmarket

# Listar tablas
\dt

# Ver esquemas
\dn

# Verificar integridad referencial
\d accounts
\d positions
\d cash_movements
\d orders
\d order_fills
\d history_events
\d user_operation_view
```

## 📝 Próximos Pasos

1. **Testear migraciones**: `mvn clean test`
2. **Levantar Docker**: `docker compose down -v && docker compose up -d --build`
3. **Verificar logs**: `docker compose logs -f`
4. **Validar endpoints**: Probar los endpoints de cada servicio
5. **Documentar datos de prueba**: Si necesitas seed data, agregarlo a `/db-init/02_seed_data.sql`

## 🛠️ Troubleshooting

### Error: "relation does not exist"
- Verifica que `01_schema.sql` se ejecutó: `docker compose logs postgres`
- Asegúrate que el volumen está correcto en `docker-compose.yml`

### Error: "permission denied"
- Verifica que el script SQL tiene permisos de lectura (`:ro`)
- Reinicia: `docker compose down -v && docker compose up -d --build`

### Las tablas existen pero Hibernate no las ve
- Verifica `ddl-auto: validate` en `application.yml`
- Revisa los logs de cada servicio: `docker compose logs <service-name>`
