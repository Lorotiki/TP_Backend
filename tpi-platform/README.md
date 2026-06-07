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

