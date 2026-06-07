# tpi-keycloak-config

Configuración de Keycloak para el TPI: realm base, roles, y cliente para api-gateway.

## Estructura
- `realm/tpi-realm-base.json`: realm `tpi` con roles `USER` y `ADMIN`, cliente `api-gateway`.

## Integración
Este directorio alimenta la inicialización de Keycloak en `tpi-platform/infra/docker-compose.yml`.

Los archivos del realm se montan en `/opt/keycloak/data/import` para que Keycloak los cargue automáticamente.

## Nota de seguridad
- No incluir credenciales ni usuarios en commits.
- El realm base se importa al iniciar Keycloak.
- Los usuarios de prueba se crean manualmente en consola admin o via script aparte.

