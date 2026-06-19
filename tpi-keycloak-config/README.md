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



## Qué cambié y por qué:
Saqué defaultRoles, el campo import y los campos duplicados a nivel realm (publicClient, clientAuthenticatorType) porque no son válidos ahí; eran ruido sin función.
api-gateway ahora tiene directAccessGrantsEnabled: false y serviceAccountsEnabled: true: no lo van a usar para loguear gente, solo queda configurado por si en algún momento necesitan que el gateway mismo llame a Keycloak (ej. introspección). Para validar tokens por firma ni siquiera necesita usar su secret en runtime.
Agregué tpi-client, público, con directAccessGrantsEnabled: true. Este es el que va a usar Postman: le pegás al endpoint de token con grant_type=password, client_id=tpi-client, username, password, y te devuelve el JWT. Sin esto, no había forma de sacar un token para probar nada.
El mapper de roles lo dejé con las claves estándar de Keycloak (multivalued: true es clave: sin eso, si un usuario tuviera dos roles podría no listarlos bien) y lo até a tpi-client, que es el que realmente emite tokens de usuarios.
