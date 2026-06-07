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

## 4. Propuesta de microservicios base
- `[[api-gateway]]`: único punto de entrada y enrutamiento.
- `[[auth-adapter o resource-server]]`: validación de tokens y reglas de acceso.
- `[[market-data-service]]`: cotizaciones externas y conversión de moneda.
- `[[portfolio-service]]`: saldo, tenencias y actualización de posiciones.
- `[[orders-service]]`: alta y resolución de órdenes de compra/venta.
- `[[history-service]]`: auditoría e historial consultable.

## 5. Plan de implementación por pasos
1. Modelar entidades y casos de uso mínimos del dominio.
2. Definir contratos entre servicios y endpoints públicos.
3. Implementar seguridad con Keycloak y roles `USER` / `ADMIN`.
4. Resolver consulta de cotizaciones con una API externa o datos mockeados.
5. Implementar portfolio y saldo con operaciones en ARS.
6. Implementar alta y validación de órdenes de venta.
7. Implementar el motor de emparejamiento para órdenes de compra.
8. Registrar cada operación en el historial.
9. Montar Docker Compose con todos los servicios y dependencias.
10. Preparar colección Postman para demostración final.

## 6. Commit incremental recomendado
Hacer commits pequeños, uno por hito funcional, para poder volver atrás y explicar avances en la defensa.

### Secuencia sugerida de commits
1. `chore: base del proyecto y arquitectura inicial`
2. `feat: gateway y seguridad con keycloak`
3. `feat: market data y cotizaciones públicas`
4. `feat: portfolio y saldo en ars`
5. `feat: orders con matching síncrono`
6. `feat: history y auditoria de operaciones`
7. `chore: docker compose y coleccion postman`

### Regla práctica de trabajo
- Después de cada tarea terminada: validar, commitear y dejar el plan actualizado.
- Si una tarea crece demasiado: partirla en dos commits antes de seguir.
- No mezclar infraestructura, negocio y documentación en un mismo commit si se puede evitar.

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
- [ ] Definir microservicios y responsabilidades.
- [ ] Definir modelo de datos y persistencia.
- [ ] Configurar Keycloak y roles.
- [ ] Implementar cotizaciones públicas.
- [ ] Implementar portfolio y saldo.
- [ ] Implementar órdenes de compra/venta.
- [ ] Implementar matching síncrono.
- [ ] Registrar historial de operaciones.
- [ ] Crear Docker Compose.
- [ ] Preparar Postman y guión de exposición.

## 12. Cómo reutilizar este archivo en otro proyecto
- Reemplazar `[[...]]` por nombres reales del dominio.
- Mantener la misma estructura de pasos y checklist.
- Ajustar la lista de microservicios según el contexto.
- Cambiar reglas de negocio, seguridad y entregables según el enunciado nuevo.

