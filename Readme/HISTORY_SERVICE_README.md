# Microservicio de Historial

Este documento explica el microservicio `tpi-history-service`, su estructura interna, sus endpoints y el funcionamiento del codigo principal. La idea es que sirva para estudiar el servicio y tambien para defenderlo en la exposicion del TPI.

## Objetivo del Servicio

El microservicio de historial se encarga de registrar y consultar eventos importantes del sistema.

En este proyecto, una operacion relevante puede ser:

- una orden de compra creada;
- una orden de compra ejecutada;
- una orden de compra rechazada;
- una orden de venta creada;
- una orden de venta ejecutada;
- una carga de saldo o deposito;
- cualquier evento futuro que otro microservicio quiera auditar.

Su responsabilidad principal es guardar una traza historica para que despues se pueda consultar:

- el historial completo de un usuario;
- el historial global del sistema para un usuario administrador.

## Ubicacion

El codigo esta en:

```text
tpi-history-service/
```

Dentro del proyecto general, el servicio corre por defecto en el puerto:

```text
8084
```

En Docker Compose, el gateway lo consume internamente como:

```text
http://history-service:8084
```

Y desde afuera, normalmente se accede por el gateway:

```text
http://localhost:8085
```

## Dependencias Principales

El archivo `pom.xml` incluye:

```text
spring-boot-starter-web
spring-boot-starter-validation
spring-boot-starter-data-jpa
postgresql
h2
spring-boot-starter-test
lombok
```

Que significa cada una:

- `spring-boot-starter-web`: permite crear endpoints REST.
- `spring-boot-starter-validation`: permite validar los DTOs con anotaciones como `@NotBlank` y `@NotNull`.
- `spring-boot-starter-data-jpa`: permite trabajar con entidades y repositorios JPA.
- `postgresql`: driver para conectarse a PostgreSQL en Docker.
- `h2`: base en memoria para tests.
- `spring-boot-starter-test`: herramientas de testing.
- `lombok`: reduce codigo repetitivo como getters, setters y constructores.

## Configuracion

Archivo:

```text
tpi-history-service/src/main/resources/application.yml
```

Contenido principal:

```yaml
spring:
  application:
    name: tpi-history-service
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/stockmarket}
    username: ${SPRING_DATASOURCE_USERNAME:admin}
    password: ${SPRING_DATASOURCE_PASSWORD:admin}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
    open-in-view: false

server:
  port: 8084
```

Puntos importantes:

- `ddl-auto: validate`: Hibernate no crea las tablas, solo valida que existan.
- Las tablas se crean desde los scripts SQL de `db-init`.
- La base real es PostgreSQL.
- En tests se usa H2.

## Estructura de Carpetas

```text
tpi-history-service/
  src/main/java/com/tpi/history/
    HistoryApplication.java
    controller/
      HistorialController.java
    dto/
      HistorialEventoRequest.java
      HistorialEventoResponse.java
    entity/
      HistorialEvento.java
      OperacionUsuarioView.java
    exception/
      GlobalExceptionHandler.java
    repository/
      HistorialEventoRepository.java
      OperacionUsuarioViewRepository.java
    service/
      HistorialService.java

  src/main/resources/
    application.yml

  src/test/java/com/tpi/history/
    HistorialControllerTest.java

  src/test/resources/
    application.yml
```

## Flujo General

El flujo principal es:

```text
Cliente u otro microservicio
        |
        v
POST /events
        |
        v
HistorialController
        |
        v
HistorialService.registraEvento()
        |
        v
Guarda evento completo en history_events
        |
        v
Crea una vista simplificada en user_operation_view
        |
        v
Devuelve HistorialEventoResponse
```

La tabla `history_events` guarda el evento completo.

La tabla `user_operation_view` guarda una version simplificada, pensada como modelo de lectura. Esta segunda tabla no reemplaza al historial completo, sino que funciona como una vista operativa para consultas o reportes mas simples.

## Endpoints

### 1. Registrar Evento

```http
POST /events
```

Registra un evento nuevo en el historial.

Ejemplo de body:

```json
{
  "eventType": "BUY_ORDER_EXECUTED",
  "userId": "juan@example.com",
  "orderId": "880e8400-e29b-41d4-a716-446655440001",
  "correlationId": "880e8400-e29b-41d4-a716-446655440001",
  "causationId": null,
  "payloadJson": {
    "symbol": "NVDA",
    "amountArs": 10000.00,
    "mensaje": "Orden de compra ejecutada"
  }
}
```

Respuesta esperada:

```json
{
  "eventId": "...",
  "eventType": "BUY_ORDER_EXECUTED",
  "userId": "juan@example.com",
  "orderId": "...",
  "correlationId": "...",
  "causationId": null,
  "payloadJson": {
    "symbol": "NVDA",
    "amountArs": 10000.00,
    "mensaje": "Orden de compra ejecutada"
  },
  "occurredAt": "2026-06-28T..."
}
```

### 2. Consultar Historial de Usuario

```http
GET /users/{userId}/history
```

Ejemplo:

```http
GET /users/juan@example.com/history
```

Devuelve todos los eventos asociados a ese usuario, ordenados desde el mas reciente al mas antiguo.

### 3. Consultar Historial Global

```http
GET /admin/history
```

Devuelve todos los eventos del sistema, tambien ordenados desde el mas reciente al mas antiguo.

En el sistema completo, esta ruta debe usarse con token de usuario `ADMIN` desde el gateway.

## Clases Principales

## `HistoryApplication`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/HistoryApplication.java
```

Es la clase principal del microservicio.

Su responsabilidad es levantar la aplicacion Spring Boot.

Normalmente tiene esta forma:

```java
@SpringBootApplication
public class HistoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(HistoryApplication.class, args);
    }
}
```

Cuando se ejecuta el jar o el contenedor Docker, esta clase es el punto de entrada.

## Controller

## `HistorialController`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/controller/HistorialController.java
```

Esta clase expone los endpoints REST.

Anotaciones importantes:

- `@RestController`: indica que la clase recibe requests HTTP y devuelve JSON.
- `@RequestMapping`: deja la ruta base vacia, por eso cada metodo define su path completo.
- `@PostMapping`: mapea requests HTTP POST.
- `@GetMapping`: mapea requests HTTP GET.
- `@Valid`: activa validaciones del DTO recibido.
- `@RequestBody`: convierte el JSON del body en un objeto Java.
- `@PathVariable`: toma valores desde la URL.

### Constructor

```java
public HistorialController(HistorialService historialService) {
    this.historialService = historialService;
}
```

Spring inyecta `HistorialService` automaticamente.

El controller no tiene logica de negocio: solo recibe la request y delega al servicio.

### `registraEvento`

```java
@PostMapping("/events")
public HistorialEventoResponse registraEvento(@Valid @RequestBody HistorialEventoRequest request) {
    return historialService.registraEvento(request);
}
```

Que hace:

1. Recibe un JSON con los datos del evento.
2. Lo convierte en `HistorialEventoRequest`.
3. Valida campos obligatorios.
4. Llama a `historialService.registraEvento`.
5. Devuelve el evento guardado como `HistorialEventoResponse`.

Este endpoint es usado internamente por otros microservicios, por ejemplo `orders-service` y `portfolio-service`, para registrar eventos.

### `getHistorialUsuario`

```java
@GetMapping("/users/{userId}/history")
public List<HistorialEventoResponse> getHistorialUsuario(@PathVariable String userId) {
    return historialService.getHistorialUsuario(userId);
}
```

Que hace:

1. Recibe el `userId` desde la URL.
2. Llama al servicio.
3. Devuelve la lista de eventos de ese usuario.

Ejemplo:

```http
GET /users/juan@example.com/history
```

### `getHistorialCompleto`

```java
@GetMapping("/admin/history")
public List<HistorialEventoResponse> getHistorialCompleto() {
    return historialService.getHistorialCompleto();
}
```

Que hace:

1. No recibe usuario.
2. Consulta todos los eventos del sistema.
3. Devuelve la lista completa.

Este endpoint representa el requerimiento del TP: "Consultar el historial completo de transacciones realizadas para el usuario ADMIN".

## Service

## `HistorialService`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/service/HistorialService.java
```

Esta es la clase mas importante del microservicio. Contiene la logica de negocio.

Anotacion:

```java
@Service
```

Esto le dice a Spring que esta clase es un componente de servicio y que puede ser inyectada en otras clases.

### Atributos

```java
private final HistorialEventoRepository historialEventoRepository;
private final OperacionUsuarioViewRepository operacionUsuarioViewRepository;
```

El servicio usa dos repositorios:

- `HistorialEventoRepository`: guarda y consulta eventos completos.
- `OperacionUsuarioViewRepository`: guarda una vista simplificada de operaciones por usuario.

### Constructor

```java
public HistorialService(HistorialEventoRepository historialEventoRepository,
                        OperacionUsuarioViewRepository operacionUsuarioViewRepository) {
    this.historialEventoRepository = historialEventoRepository;
    this.operacionUsuarioViewRepository = operacionUsuarioViewRepository;
}
```

Spring usa este constructor para inyectar los repositorios.

Al estar marcados como `final`, quedan inicializados una sola vez.

### `registraEvento`

```java
@Transactional
public HistorialEventoResponse registraEvento(HistorialEventoRequest request)
```

Es el metodo que registra un evento nuevo.

Paso a paso:

1. Crea una entidad `HistorialEvento`.

```java
var evento = new HistorialEvento();
```

2. Define el ID del evento.

```java
evento.setEventId(request.eventId() != null ? request.eventId() : UUID.randomUUID());
```

Si el request trae `eventId`, lo usa. Si no, genera uno nuevo.

3. Copia los campos principales desde el DTO hacia la entidad.

```java
evento.setEventType(request.eventType());
evento.setUserId(request.userId());
evento.setOrderId(request.orderId());
evento.setCorrelationId(request.correlationId());
evento.setCausationId(request.causationId());
evento.setPayloadJson(request.payloadJson());
```

4. Guarda el evento completo en la base.

```java
var historialEventoGrabado = historialEventoRepository.save(evento);
```

Esto inserta una fila en la tabla:

```text
history_events
```

5. Genera una vista simplificada.

```java
persistModeloLectura(historialEventoGrabado);
```

Esto inserta una fila en:

```text
user_operation_view
```

6. Devuelve un DTO de respuesta.

```java
return toResponse(historialEventoGrabado);
```

La anotacion `@Transactional` es importante: si algo falla en el guardado del evento o en el guardado de la vista, se hace rollback y no queda la base en estado inconsistente.

### `getHistorialUsuario`

```java
public List<HistorialEventoResponse> getHistorialUsuario(String userId)
```

Busca eventos de un usuario particular.

Codigo:

```java
return historialEventoRepository.findByUserIdOrderByOccurredAtDesc(userId).stream()
        .map(this::toResponse)
        .toList();
```

Que hace:

1. Llama al repositorio para buscar eventos por `userId`.
2. Los ordena por fecha descendente.
3. Convierte cada entidad `HistorialEvento` en `HistorialEventoResponse`.
4. Devuelve la lista.

El uso de `stream()` permite transformar la lista de entidades en una lista de DTOs.

### `getHistorialCompleto`

```java
public List<HistorialEventoResponse> getHistorialCompleto()
```

Busca todos los eventos del sistema.

Codigo:

```java
return historialEventoRepository.findAllByOrderByOccurredAtDesc().stream()
        .map(this::toResponse)
        .toList();
```

Que hace:

1. Consulta todos los registros de `history_events`.
2. Los ordena por `occurredAt` descendente.
3. Los convierte a DTO de respuesta.
4. Devuelve la lista.

### `persistModeloLectura`

```java
private void persistModeloLectura(HistorialEvento historialEventoGuardado)
```

Este metodo privado crea una fila resumida en `user_operation_view`.

No es un endpoint, es una funcion interna del servicio.

Primero valida si el evento tiene usuario:

```java
if (historialEventoGuardado.getUserId() == null || historialEventoGuardado.getUserId().isBlank()) {
    return;
}
```

Si no hay usuario, no guarda vista simplificada.

Despues crea una entidad `OperacionUsuarioView`:

```java
var item = new OperacionUsuarioView();
```

Asigna datos principales:

```java
item.setUserId(historialEventoGuardado.getUserId());
item.setOperationType(historialEventoGuardado.getEventType());
```

Extrae datos desde el JSON dinamico:

```java
item.setSymbol((String) historialEventoGuardado.getPayloadJson().get("symbol"));
item.setAmountArs(extraerMonto(historialEventoGuardado.getPayloadJson().get("amountArs")));
```

Finalmente guarda:

```java
operacionUsuarioViewRepository.save(item);
```

Este metodo implementa una idea parecida a CQRS:

- `history_events`: modelo completo de escritura y auditoria.
- `user_operation_view`: modelo de lectura resumido.

### `extraerMonto`

```java
private BigDecimal extraerMonto(Object valor)
```

Este metodo convierte un valor dinamico del `payloadJson` en `BigDecimal`.

Por que hace falta:

El campo `payloadJson` es un `Map<String, Object>`, entonces `amountArs` puede llegar como:

- `Integer`;
- `Double`;
- `Long`;
- `BigDecimal`;
- `String`;
- `null`.

El metodo lo normaliza.

Si es `null`:

```java
if (valor == null) {
    return null;
}
```

Si es un numero:

```java
if (valor instanceof Number number) {
    return BigDecimal.valueOf(number.doubleValue())
            .setScale(2, java.math.RoundingMode.HALF_UP);
}
```

Lo convierte a `BigDecimal` con 2 decimales.

Si viene como texto:

```java
return new BigDecimal(valor.toString())
        .setScale(2, java.math.RoundingMode.HALF_UP);
```

Ejemplos:

```text
100       -> 100.00
100.5     -> 100.50
"2500.30" -> 2500.30
null      -> null
```

### `toResponse`

```java
private HistorialEventoResponse toResponse(HistorialEvento event)
```

Convierte una entidad JPA en un DTO de respuesta.

Codigo:

```java
return new HistorialEventoResponse(
        event.getEventId(),
        event.getEventType(),
        event.getUserId(),
        event.getOrderId(),
        event.getCorrelationId(),
        event.getCausationId(),
        event.getPayloadJson(),
        event.getOccurredAt()
);
```

Por que se usa:

- Evita devolver directamente la entidad JPA.
- Permite controlar que datos salen por la API.
- Separa capa de persistencia de capa HTTP.

## DTOs

## `HistorialEventoRequest`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/dto/HistorialEventoRequest.java
```

Representa el JSON que entra por `POST /events`.

Campos:

```java
UUID eventId
String eventType
String userId
UUID orderId
UUID correlationId
UUID causationId
Map<String, Object> payloadJson
```

Validaciones:

```java
@NotBlank String eventType
@NotNull Map<String, Object> payloadJson
```

Esto significa:

- `eventType` es obligatorio y no puede venir vacio.
- `payloadJson` es obligatorio.

Campos como `userId`, `orderId`, `correlationId` y `causationId` pueden venir en `null`.

## `HistorialEventoResponse`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/dto/HistorialEventoResponse.java
```

Representa el JSON que devuelve el servicio.

Campos:

```java
UUID eventId
String eventType
String userId
UUID orderId
UUID correlationId
UUID causationId
Map<String, Object> payloadJson
LocalDateTime occurredAt
```

Es muy parecido al request, pero agrega `occurredAt`, que indica cuando ocurrio o se guardo el evento.

## Entidades

## `HistorialEvento`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/entity/HistorialEvento.java
```

Mapea contra la tabla:

```text
history_events
```

Anotaciones:

- `@Entity`: indica que es una entidad JPA.
- `@Table(name = "history_events")`: define la tabla.
- `@Id`: marca la clave primaria.
- `@Column`: configura columnas.
- `@JdbcTypeCode(SqlTypes.JSON)`: permite guardar un `Map<String, Object>` como JSON en la base.
- `@PrePersist`: ejecuta codigo antes de insertar.
- `@Data`: Lombok genera getters, setters, `toString`, etc.
- `@AllArgsConstructor`: genera constructor con todos los campos.
- `@NoArgsConstructor`: genera constructor vacio requerido por JPA.

Campos:

```java
private UUID eventId;
private String eventType;
private String userId;
private UUID orderId;
private UUID correlationId;
private UUID causationId;
private Map<String, Object> payloadJson;
private LocalDateTime occurredAt;
```

### `onCreate`

```java
@PrePersist
void onCreate() {
    if (eventId == null) {
        eventId = UUID.randomUUID();
    }
    if (occurredAt == null) {
        occurredAt = LocalDateTime.now();
    }
}
```

Que hace:

- Si no hay `eventId`, genera uno.
- Si no hay `occurredAt`, pone la fecha y hora actual.

Esto hace que el servicio sea mas robusto: aunque el request no traiga ID o fecha, la entidad se completa antes de persistirse.

## `OperacionUsuarioView`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/entity/OperacionUsuarioView.java
```

Mapea contra la tabla:

```text
user_operation_view
```

Es una version resumida del evento.

Campos:

```java
private UUID id;
private String userId;
private String operationType;
private String symbol;
private BigDecimal amountArs;
private LocalDateTime createdAt;
```

Se usa para guardar datos mas simples de cada operacion:

- usuario;
- tipo de operacion;
- simbolo;
- monto;
- fecha.

### `onCreate`

```java
@PrePersist
void onCreate() {
    if (id == null) {
        id = UUID.randomUUID();
    }
    if (createdAt == null) {
        createdAt = LocalDateTime.now();
    }
}
```

Antes de guardar:

- genera `id` si no existe;
- genera `createdAt` si no existe.

## Repositorios

## `HistorialEventoRepository`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/repository/HistorialEventoRepository.java
```

Extiende:

```java
JpaRepository<HistorialEvento, UUID>
```

Esto le da metodos como:

- `save`;
- `findById`;
- `findAll`;
- `delete`;
- `count`.

Metodos personalizados:

```java
List<HistorialEvento> findByUserIdOrderByOccurredAtDesc(String userId);
```

Busca eventos por usuario y los ordena por fecha descendente.

```java
List<HistorialEvento> findAllByOrderByOccurredAtDesc();
```

Busca todos los eventos y los ordena por fecha descendente.

Spring Data JPA interpreta el nombre del metodo y genera la consulta automaticamente.

## `OperacionUsuarioViewRepository`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/repository/OperacionUsuarioViewRepository.java
```

Extiende:

```java
JpaRepository<OperacionUsuarioView, UUID>
```

Hoy no define metodos personalizados.

Se usa principalmente para:

```java
operacionUsuarioViewRepository.save(item);
```

Es decir, para guardar la vista simplificada de cada evento.

## Manejo de Errores

## `GlobalExceptionHandler`

Ubicacion:

```text
tpi-history-service/src/main/java/com/tpi/history/exception/GlobalExceptionHandler.java
```

Anotaciones:

- `@RestControllerAdvice`: intercepta errores de los controllers y responde JSON.
- `@ExceptionHandler`: define que metodo maneja que tipo de error.
- `@Slf4j`: Lombok agrega un logger llamado `log`.

### `handleResponseStatusException`

```java
@ExceptionHandler(ResponseStatusException.class)
public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e)
```

Maneja errores controlados, por ejemplo un `400`, `404` o `403` lanzado manualmente.

Devuelve:

```json
{
  "code": 400,
  "message": "Error en la solicitud",
  "timestamp": "..."
}
```

### `handleGeneralException`

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleGeneralException(Exception e)
```

Maneja cualquier excepcion inesperada.

Devuelve siempre:

```json
{
  "code": 500,
  "message": "Error interno del servidor",
  "timestamp": "..."
}
```

### `ErrorResponse`

En este servicio, `ErrorResponse` esta declarado como un `record` dentro del mismo archivo `GlobalExceptionHandler.java`.

```java
record ErrorResponse(
        int code,
        String message,
        OffsetDateTime timestamp
) {}
```

Sirve para estandarizar la respuesta de error.

## Base de Datos

El servicio usa dos tablas principales.

### `history_events`

Guarda el evento completo.

Columnas principales:

```text
event_id
event_type
user_id
order_id
correlation_id
causation_id
payload_json
occurred_at
```

Uso:

- auditoria completa;
- trazabilidad;
- historial por usuario;
- historial global.

### `user_operation_view`

Guarda una version resumida.

Columnas principales:

```text
id
user_id
operation_type
symbol
amount_ars
created_at
```

Uso:

- consultas simplificadas;
- reportes;
- modelo de lectura.

## Relacion con Otros Microservicios

Este servicio no ejecuta compras ni ventas. Solo registra lo que otros servicios le informan.

Ejemplos:

### `orders-service`

Cuando se crea o ejecuta una orden, `orders-service` llama:

```http
POST http://history-service:8084/events
```

Eventos posibles:

```text
SELL_ORDER_CREATED
SELL_ORDER_EXECUTED
BUY_ORDER_EXECUTED
BUY_ORDER_REJECTED
```

### `portfolio-service`

Cuando se carga saldo o se registra un deposito, `portfolio-service` puede llamar:

```http
POST http://history-service:8084/events
```

Evento posible:

```text
DEPOSIT_RECEIVED
```

## Ejemplos de Uso

### Registrar evento de deposito

```http
POST /events
Content-Type: application/json
```

```json
{
  "eventType": "DEPOSIT_RECEIVED",
  "userId": "juan@example.com",
  "payloadJson": {
    "amountArs": 250000.00,
    "referenceId": "carga-saldo-demo-1",
    "mensaje": "Deposito acreditado"
  }
}
```

### Registrar evento de compra ejecutada

```json
{
  "eventType": "BUY_ORDER_EXECUTED",
  "userId": "juan@example.com",
  "orderId": "880e8400-e29b-41d4-a716-446655440001",
  "correlationId": "880e8400-e29b-41d4-a716-446655440001",
  "payloadJson": {
    "symbol": "NVDA",
    "amountArs": 35000.00,
    "cantidad": 1,
    "mensaje": "Orden de compra ejecutada"
  }
}
```

### Consultar historial de usuario

```http
GET /users/juan@example.com/history
```

### Consultar historial global

```http
GET /admin/history
```

## Test del Servicio

Archivo:

```text
tpi-history-service/src/test/java/com/tpi/history/HistorialControllerTest.java
```

El test principal se llama:

```java
shouldRegisterAndRetrieveHistory()
```

Que valida:

1. Que se pueda registrar un evento con `POST /events`.
2. Que el evento devuelva `eventType` correcto.
3. Que despues se pueda consultar con `GET /users/user-1/history`.
4. Que el historial devuelva el usuario esperado.

Para tests se usa H2 con esta configuracion:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:history;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  jpa:
    hibernate:
      ddl-auto: create-drop
```

Esto permite probar sin levantar PostgreSQL.

## Resumen para Exponer

El microservicio de historial cumple el rol de auditoria del sistema.

Sus responsabilidades son:

- recibir eventos desde otros microservicios;
- guardar el evento completo en `history_events`;
- crear una vista resumida en `user_operation_view`;
- permitir consultar historial por usuario;
- permitir consultar historial global para administradores.

El metodo central es:

```java
registraEvento()
```

Porque coordina:

1. conversion de DTO a entidad;
2. persistencia del evento completo;
3. persistencia del modelo de lectura;
4. conversion de entidad a DTO de respuesta.

En terminos de arquitectura, este servicio ayuda a separar responsabilidades: `orders-service` y `portfolio-service` no necesitan manejar consultas historicas complejas; simplemente informan eventos, y `history-service` se encarga de guardarlos y exponerlos.
