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
- [ ] Fase 1 completa: dominio y contratos.
- [ ] Fase 2 completa: seguridad e infraestructura.
- [ ] Fase 3 completa: cotizaciones y portfolio.
- [ ] Fase 4 completa: órdenes y matching.
- [ ] Fase 5 completa: historial y demo.

### Indicador simple de progreso
- `x/5 fases completadas`
- `x/5 entregables principales listos`

## 12. Cómo reutilizar este archivo en otro proyecto
- Reemplazar `[[...]]` por nombres reales del dominio.
- Mantener la misma estructura de pasos y checklist.
- Ajustar la lista de microservicios según el contexto.
- Cambiar reglas de negocio, seguridad y entregables según el enunciado nuevo.

