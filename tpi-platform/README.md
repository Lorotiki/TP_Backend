# tpi-platform

Infraestructura base local para el TPI con una base por microservicio.

## Estructura
- `infra/docker-compose.yml`
- `infra/db/portfolio/init`
- `infra/db/orders/init`
- `infra/db/history/init`

## Requisitos
- Docker
- Docker Compose plugin (`docker compose`)

## Uso rapido
```bash
cp .env.example .env
make up
make ps
```

## Ver logs
```bash
make logs
```

## Bajar servicios
```bash
make down
```

## Reiniciar limpiando volumenes
```bash
make reset
```

## Puertos por defecto
- portfolio-db: `5433`
- orders-db: `5434`
- history-db: `5435`

## Notas
- Los scripts SQL se ejecutan automaticamente al crear el volumen por primera vez.
- Si cambias scripts y queres reinicializar, usa `make reset` y luego `make up`.

## Keycloak
### Acceso a la consola admin
1. Levantar servicios: `make up`
2. Esperar a que Keycloak esté listo (~30 segundos)
3. Navegar a `http://localhost:8080`
4. Ir a "Administration Console"
5. Usuario: `admin` (definido en `.env`)
6. Contraseña: `admin` (definido en `.env`)

### Realm y roles
- El realm `tpi` se importa automáticamente desde `tpi-keycloak-config/realm/tpi-realm-base.json`
- Roles disponibles: `USER` y `ADMIN`
- Cliente técnico: `api-gateway` (para validar tokens en el gateway)

### Crear un usuario de prueba
1. En consola admin, ir a `Users`
2. Crear usuario (ej: `user1`)
3. Asignar rol `USER` o `ADMIN`
4. Establecer contraseña temporal

