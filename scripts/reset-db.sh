#!/usr/bin/env bash
# =============================================================================
# reset-db.sh — Reinicia la base de datos de stockmarket al estado inicial
#
# Uso:
#   ./scripts/reset-db.sh          → limpia datos y recarga seed (Opción A)
#   ./scripts/reset-db.sh --full   → borra volúmenes y levanta todo de cero (Opción C)
#
# Ejecutar desde la raíz del proyecto.
# =============================================================================

set -euo pipefail
cd "$(dirname "$0")/.."

COMPOSE_FILE="docker-compose.yml"
DB_CONTAINER="tp-postgres-1"
DB_USER="admin"
DB_NAME="stockmarket"

case "${1:-}" in
  --full)
    echo "==> Reset COMPLETO: bajando contenedores y borrando volúmenes..."
    docker compose -f "$COMPOSE_FILE" down -v
    echo "==> Levantando todo de cero..."
    docker compose -f "$COMPOSE_FILE" up -d
    echo ""
    echo "==> Esperando a que Postgres esté listo..."
    for i in $(seq 1 30); do
      if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" -d "$DB_NAME" &>/dev/null; then
        echo "==> Postgres listo."
        break
      fi
      sleep 2
    done
    echo ""
    echo "==> Reset completo terminado. Keycloak puede tardar ~30s más en estar disponible."
    ;;
  *)
    echo "==> Limpiando datos de negocio y recargando seed..."
    docker exec -it "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "
      TRUNCATE order_fills, orders, history_events, user_operation_view,
               cash_movements, positions, audit_log, accounts
      RESTART IDENTITY CASCADE;
    "
    echo "==> Recargando datos iniciales desde init.sql..."
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < db-init/init.sql
    echo ""
    echo "==> Base de datos reiniciada al estado inicial."
    echo "    Usuarios disponibles: juan, maria, carlos, ana, diego, lucia, pedro, sofia"
    echo "    (todos en @example.com)"
    ;;
esac

