# Microservicio Portfolio

Este README explica el microservicio `tpi-portfolio-service`: que responsabilidad tiene, como esta organizado, que endpoints expone y que hace cada metodo importante del codigo.

## Responsabilidad general

El microservicio de portfolio se encarga de administrar la cartera de cada usuario.

Sus responsabilidades principales son:

- Crear una cuenta de portfolio para un usuario si todavia no existe.
- Consultar el saldo disponible en pesos argentinos.
- Consultar las posiciones del usuario, es decir, que acciones tiene y cuantas.
- Cargar saldo al portfolio.
- Aplicar compras y ventas cuando otro servicio confirma una operacion.
- Registrar movimientos de dinero.
- Avisar al microservicio de historial cuando se acredita un deposito.

En terminos simples: este servicio guarda cuanto dinero tiene un usuario y que acciones posee.

## Ubicacion dentro del proyecto

```text
tpi-portfolio-service/
+-- src/main/java/com/tpi/portfolio/
|   +-- PortfolioApplication.java
|   +-- config/
|   |   +-- RestClientsConfig.java
|   +-- controller/
|   |   +-- PortafolioController.java
|   +-- dto/
|   |   +-- DepositoRequest.java
|   |   +-- DepositoResponse.java
|   |   +-- HistorialEventoClienteRequest.java
|   |   +-- PortafolioResponse.java
|   |   +-- PosicionResponse.java
|   |   +-- TradeAdjustmentRequest.java
|   +-- entity/
|   |   +-- Cuenta.java
|   |   +-- MovimientoDinero.java
|   |   +-- Posicion.java
|   +-- exception/
|   |   +-- GlobalExceptionHandler.java
|   +-- repository/
|   |   +-- CuentaRepository.java
|   |   +-- MovimientoDineroRepository.java
|   |   +-- PosicionRepository.java
|   +-- service/
|       +-- PortafolioService.java
+-- src/main/resources/
    +-- application.yml
```

## Capas del microservicio

El servicio esta dividido en capas:

- `controller`: recibe las peticiones HTTP.
- `service`: contiene la logica de negocio.
- `repository`: se comunica con la base de datos usando Spring Data JPA.
- `entity`: representa las tablas de la base de datos.
- `dto`: define los objetos que entran y salen por la API.
- `config`: contiene configuraciones de Spring.
- `exception`: centraliza el manejo de errores.

## Endpoints del controller

El controller principal es `PortafolioController`.

La ruta base es:

```java
@RequestMapping("/users/{userId}")
```

Eso significa que todos los endpoints reciben un `userId` en la URL.

### GET `/users/{userId}/portfolio`

Consulta el portfolio de un usuario.

Ejemplo:

```http
GET /users/juan@example.com/portfolio
```

Metodo del controller:

```java
public PortafolioResponse getPortafolio(@PathVariable String userId)
```

Que hace:

- Toma el `userId` desde la URL.
- Llama a `portafolioService.getPortafolio(userId)`.
- Devuelve el saldo y las posiciones del usuario.

Respuesta esperada:

```json
{
  "userId": "juan@example.com",
  "balanceArs": 250000.00,
  "positions": [
    {
      "symbol": "AAPL",
      "quantity": 10,
      "avgPriceArs": 15000.00
    }
  ]
}
```

### POST `/users/{userId}/deposits`

Carga saldo al portfolio del usuario.

Ejemplo:

```http
POST /users/juan@example.com/deposits
```

Body:

```json
{
  "amountArs": 250000.00,
  "referenceId": "deposito-1"
}
```

Metodo del controller:

```java
public DepositoResponse deposito(@PathVariable String userId, @Valid @RequestBody DepositoRequest request)
```

Que hace:

- Toma el `userId` desde la URL.
- Lee el JSON del body y lo transforma en `DepositoRequest`.
- Valida el request con `@Valid`.
- Llama a `portafolioService.deposito(userId, request)`.
- Devuelve el ID del movimiento y el nuevo saldo.

### POST `/users/{userId}/portfolio/balance`

Tambien carga saldo al portfolio.

Ejemplo:

```http
POST /users/juan@example.com/portfolio/balance
```

Body:

```json
{
  "amountArs": 250000.00,
  "referenceId": "carga-saldo-demo-1"
}
```

Metodo del controller:

```java
public DepositoResponse cargarSaldo(@PathVariable String userId, @Valid @RequestBody DepositoRequest request)
```

Este endpoint hace lo mismo que `/deposits`. Se agrego para que el flujo de Postman pueda cargar saldo usando una URL mas directa sobre el portfolio.

Internamente tambien llama a:

```java
portafolioService.deposito(userId, request)
```

### POST `/users/{userId}/trades`

Aplica una compra o venta ya confirmada.

Ejemplo:

```http
POST /users/juan@example.com/trades
```

Body de compra:

```json
{
  "side": "BUY",
  "symbol": "AAPL",
  "quantity": 10,
  "priceArs": 15000.00,
  "referenceId": "trade-1"
}
```

Body de venta:

```json
{
  "side": "SELL",
  "symbol": "AAPL",
  "quantity": 5,
  "priceArs": 16000.00,
  "referenceId": "trade-2"
}
```

Metodo del controller:

```java
public PortafolioResponse applyTrade(@PathVariable String userId, @Valid @RequestBody TradeAdjustmentRequest request)
```

Que hace:

- Recibe una compra o venta.
- Valida que los datos no vengan vacios.
- Llama a `portafolioService.aplicarTrade(userId, request)`.
- Devuelve el portfolio actualizado.

Este endpoint normalmente lo usa otro microservicio, por ejemplo `orders-service`, cuando una orden se ejecuta.

## Service principal: `PortafolioService`

`PortafolioService` contiene la logica importante del microservicio.

Tiene estas dependencias:

```java
private final CuentaRepository cuentaRepository;
private final PosicionRepository posicionRepository;
private final MovimientoDineroRepository movimientoDineroRepository;
private final RestTemplate restTemplate;
private final String historialUrl;
```

Cada una cumple una funcion:

- `CuentaRepository`: busca y guarda cuentas.
- `PosicionRepository`: busca y guarda posiciones de acciones.
- `MovimientoDineroRepository`: guarda movimientos de dinero.
- `RestTemplate`: permite llamar a otro microservicio por HTTP.
- `historialUrl`: URL base del microservicio de historial.

### `getPortafolio(String userId)`

```java
@Transactional
public PortafolioResponse getPortafolio(String userId)
```

Este metodo devuelve el portfolio de un usuario.

Paso a paso:

1. Busca la cuenta del usuario.
2. Si la cuenta no existe, la crea con saldo cero.
3. Busca todas las posiciones asociadas a esa cuenta.
4. Convierte cada `Posicion` en `PosicionResponse`.
5. Devuelve un `PortafolioResponse`.

Parte importante:

```java
Cuenta cuenta = findOrCreateAccount(userId);
```

Esto evita errores si el usuario todavia no tiene cuenta creada.

Despues:

```java
List<Posicion> posiciones = posicionRepository.findByAccountId(cuenta.getId());
```

Busca las acciones que tiene esa cuenta.

Luego:

```java
List<PosicionResponse> posicionResponses = posiciones.stream()
        .map(p -> new PosicionResponse(p.getSymbol(), p.getQuantity(), p.getAvgPriceArs()))
        .collect(Collectors.toList());
```

Convierte entidades de base de datos en DTOs de respuesta.

### `deposito(String userId, DepositoRequest request)`

```java
@Transactional
public DepositoResponse deposito(String userId, DepositoRequest request)
```

Este metodo carga saldo en pesos argentinos a la cuenta de un usuario.

Paso a paso:

1. Busca o crea la cuenta.
2. Valida que el monto sea positivo.
3. Suma el monto al saldo actual.
4. Guarda la cuenta actualizada.
5. Crea un movimiento de dinero de tipo `DEPOSIT`.
6. Guarda ese movimiento en la tabla `cash_movements`.
7. Registra el deposito en el microservicio de historial.
8. Devuelve el ID del movimiento y el nuevo saldo.

Validacion del monto:

```java
if (request.amountArs().compareTo(BigDecimal.ZERO) <= 0) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto del deposito debe ser positivo.");
}
```

Si el monto es cero o negativo, devuelve error `400 BAD REQUEST`.

Suma del saldo:

```java
cuenta.setBalanceArs(cuenta.getBalanceArs().add(request.amountArs()));
```

Creacion del movimiento:

```java
MovimientoDinero movement = new MovimientoDinero();
movement.setAccountId(cuenta.getId());
movement.setType("DEPOSIT");
movement.setAmountArs(request.amountArs());
movement.setReferenceId(request.referenceId());
```

Ese movimiento sirve para dejar registro interno de que hubo una carga de dinero.

### `aplicarTrade(String userId, TradeAdjustmentRequest request)`

```java
@Transactional
public PortafolioResponse aplicarTrade(String userId, TradeAdjustmentRequest request)
```

Este metodo actualiza el portfolio cuando se aplica una compra o una venta.

Primero calcula el monto total:

```java
BigDecimal totalAmount = request.priceArs().multiply(request.quantity());
```

Por ejemplo:

- precio: `15000`
- cantidad: `10`
- total: `150000`

Despues mira si el trade es `BUY` o `SELL`.

### Caso compra: `BUY`

Si el usuario compra:

1. Verifica que tenga saldo suficiente.
2. Resta el dinero del saldo.
3. Busca si ya existe una posicion para ese simbolo.
4. Si no existe, crea una posicion nueva.
5. Suma la cantidad comprada.
6. Recalcula el precio promedio.
7. Guarda la posicion.

Validacion de saldo:

```java
if (cuenta.getBalanceArs().compareTo(totalAmount) < 0) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saldo insuficiente para realizar la compra.");
}
```

Si no alcanza el dinero, devuelve error `400`.

Resta del saldo:

```java
cuenta.setBalanceArs(cuenta.getBalanceArs().subtract(totalAmount));
```

Busqueda o creacion de posicion:

```java
Posicion posicion = posicionRepository.findByAccountIdAndSymbol(cuenta.getId(), request.symbol())
        .orElseGet(() -> {
            Posicion nuevaPosicion = new Posicion();
            nuevaPosicion.setAccountId(cuenta.getId());
            nuevaPosicion.setSymbol(request.symbol());
            nuevaPosicion.setQuantity(BigDecimal.ZERO);
            nuevaPosicion.setAvgPriceArs(BigDecimal.ZERO);
            return nuevaPosicion;
        });
```

Esto significa: si el usuario ya tenia esa accion, actualiza esa posicion; si no la tenia, crea una nueva.

Recalculo del precio promedio:

```java
BigDecimal newQuantity = posicion.getQuantity().add(request.quantity());
BigDecimal newTotalValue = (posicion.getAvgPriceArs().multiply(posicion.getQuantity())).add(totalAmount);
BigDecimal newAvgPrice = newTotalValue.divide(newQuantity, 4, RoundingMode.HALF_UP);
```

Ejemplo:

- Tenia 10 acciones a promedio 1000.
- Compra 10 acciones mas a 2000.
- Ahora tiene 20 acciones.
- Promedio nuevo: 1500.

### Caso venta: `SELL`

Si el usuario vende:

1. Busca la posicion del simbolo.
2. Si no tiene esa accion, devuelve error.
3. Verifica que tenga cantidad suficiente.
4. Resta la cantidad vendida.
5. Suma el dinero al saldo.
6. Si la cantidad queda en cero, elimina la posicion.
7. Si todavia quedan acciones, guarda la posicion actualizada.

Busqueda de posicion:

```java
Posicion posicion = posicionRepository.findByAccountIdAndSymbol(cuenta.getId(), request.symbol())
        .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No posee acciones del simbolo " + request.symbol() + " para vender."
        ));
```

Validacion de cantidad:

```java
if (posicion.getQuantity().compareTo(request.quantity()) < 0) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad de acciones insuficiente para vender.");
}
```

Actualizacion:

```java
posicion.setQuantity(posicion.getQuantity().subtract(request.quantity()));
cuenta.setBalanceArs(cuenta.getBalanceArs().add(totalAmount));
```

Si vendio todo:

```java
if (posicion.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
    posicionRepository.delete(posicion);
}
```

Esto evita dejar posiciones con cantidad cero.

### `findOrCreateAccount(String userId)`

```java
private Cuenta findOrCreateAccount(String userId)
```

Este metodo privado busca una cuenta por `userId`.

Si existe, la devuelve.

Si no existe, crea una nueva:

```java
Cuenta nuevaCuenta = new Cuenta();
nuevaCuenta.setUserId(userId);
nuevaCuenta.setBalanceArs(BigDecimal.ZERO);
return cuentaRepository.save(nuevaCuenta);
```

Es importante porque permite que el primer `GET /portfolio` de un usuario funcione aunque todavia no tenga datos cargados.

### `registrarDepositoEnHistorial(...)`

```java
private void registrarDepositoEnHistorial(String userId, DepositoRequest request, String movementId, BigDecimal nuevoSaldo)
```

Este metodo avisa al microservicio de historial que se acredito un deposito.

Arma un `payload` con datos del deposito:

```java
payload.put("amountArs", request.amountArs());
payload.put("referenceId", request.referenceId());
payload.put("movementId", movementId);
payload.put("balanceArs", nuevoSaldo);
payload.put("mensaje", "Deposito acreditado");
```

Despues crea un `HistorialEventoClienteRequest`:

```java
HistorialEventoClienteRequest historialRequest = new HistorialEventoClienteRequest(
        eventId,
        "DEPOSIT_RECEIVED",
        userId,
        null,
        eventId,
        null,
        payload
);
```

Y lo envia al history-service:

```java
restTemplate.postForObject(historialUrl + "/events", historialRequest, Void.class);
```

El metodo esta dentro de un `try/catch`.

Eso significa que si falla el historial, el deposito no se cae. El saldo igual queda cargado, pero se registra un warning en logs.

## Entidades

Las entidades representan tablas de la base de datos.

### `Cuenta`

Archivo:

```text
entity/Cuenta.java
```

Tabla:

```java
@Table(name = "accounts")
```

Representa la cuenta de portfolio de un usuario.

Campos principales:

- `id`: identificador UUID de la cuenta.
- `userId`: usuario al que pertenece la cuenta.
- `balanceArs`: saldo disponible en pesos argentinos.
- `createdAt`: fecha de creacion.
- `updatedAt`: fecha de ultima modificacion.

Metodo `onCreate()`:

```java
@PrePersist
void onCreate()
```

Se ejecuta antes de guardar por primera vez la entidad.

Hace tres cosas:

- Genera un UUID si no hay `id`.
- Inicializa el saldo en cero si viene nulo.
- Completa `createdAt` y `updatedAt`.

Metodo `onUpdate()`:

```java
@PreUpdate
void onUpdate()
```

Se ejecuta antes de actualizar la entidad.

Actualiza:

```java
updatedAt = LocalDateTime.now();
```

### `Posicion`

Archivo:

```text
entity/Posicion.java
```

Tabla:

```java
@Table(name = "positions")
```

Representa una accion que posee el usuario.

Campos principales:

- `id`: identificador UUID de la posicion.
- `accountId`: cuenta a la que pertenece.
- `symbol`: simbolo de la accion, por ejemplo `AAPL`, `NVDA`, `AMD`.
- `quantity`: cantidad de acciones.
- `avgPriceArs`: precio promedio en pesos argentinos.
- `updatedAt`: fecha de ultima modificacion.

Ejemplo conceptual:

```text
Cuenta de juan@example.com
  - AAPL: 10 acciones, promedio 15000 ARS
  - AMD: 5 acciones, promedio 20000 ARS
```

### `MovimientoDinero`

Archivo:

```text
entity/MovimientoDinero.java
```

Tabla:

```java
@Table(name = "cash_movements")
```

Representa un movimiento de dinero.

Campos principales:

- `id`: identificador UUID del movimiento.
- `accountId`: cuenta relacionada.
- `type`: tipo de movimiento, por ahora `DEPOSIT`.
- `amountArs`: monto en pesos argentinos.
- `referenceId`: referencia externa o identificador del flujo.
- `createdAt`: fecha de creacion.

Cuando se hace un deposito, se guarda un registro en esta tabla.

## DTOs

Los DTOs son objetos usados para recibir o devolver datos por la API.

### `DepositoRequest`

```java
public record DepositoRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amountArs,
        @NotBlank String referenceId
)
```

Se usa para cargar saldo.

Validaciones:

- `@NotNull`: el monto no puede venir nulo.
- `@DecimalMin("0.01")`: el monto debe ser como minimo `0.01`.
- `@NotBlank`: `referenceId` no puede venir vacio.

### `DepositoResponse`

```java
public record DepositoResponse(
        UUID movementId,
        BigDecimal balanceArs
)
```

Respuesta de una carga de saldo.

Devuelve:

- `movementId`: ID del movimiento creado.
- `balanceArs`: saldo actualizado.

### `TradeAdjustmentRequest`

```java
public record TradeAdjustmentRequest(
        @NotBlank String side,
        @NotBlank String symbol,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0.01") BigDecimal priceArs,
        @NotBlank String referenceId
)
```

Se usa para aplicar una compra o venta al portfolio.

Campos:

- `side`: `BUY` o `SELL`.
- `symbol`: simbolo de la accion.
- `quantity`: cantidad comprada o vendida.
- `priceArs`: precio en pesos argentinos.
- `referenceId`: referencia de la operacion.

### `PortafolioResponse`

```java
public record PortafolioResponse(
        String userId,
        BigDecimal balanceArs,
        List<PosicionResponse> positions
)
```

Respuesta principal del portfolio.

Devuelve:

- usuario.
- saldo.
- lista de posiciones.

### `PosicionResponse`

```java
public record PosicionResponse(
        String symbol,
        BigDecimal quantity,
        BigDecimal avgPriceArs
)
```

Representa una posicion dentro del portfolio.

### `HistorialEventoClienteRequest`

```java
public record HistorialEventoClienteRequest(
        UUID eventId,
        String eventType,
        String userId,
        UUID orderId,
        UUID correlationId,
        UUID causationId,
        Map<String, Object> payloadJson
)
```

Se usa para enviar eventos al microservicio de historial.

En este servicio se usa cuando se registra un deposito con tipo:

```text
DEPOSIT_RECEIVED
```

## Repositories

Los repositories extienden `JpaRepository`.

Eso le permite a Spring Data JPA generar automaticamente consultas comunes como guardar, buscar por ID, listar y eliminar.

### `CuentaRepository`

```java
public interface CuentaRepository extends JpaRepository<Cuenta, UUID> {
    Optional<Cuenta> findByUserId(String userId);
}
```

Metodo personalizado:

- `findByUserId(String userId)`: busca una cuenta por usuario.

Spring entiende el nombre del metodo y arma la query automaticamente.

### `PosicionRepository`

```java
public interface PosicionRepository extends JpaRepository<Posicion, UUID> {
    List<Posicion> findByAccountId(UUID accountId);
    Optional<Posicion> findByAccountIdAndSymbol(UUID accountId, String symbol);
}
```

Metodos:

- `findByAccountId`: trae todas las posiciones de una cuenta.
- `findByAccountIdAndSymbol`: busca una posicion puntual de una cuenta y un simbolo.

### `MovimientoDineroRepository`

```java
public interface MovimientoDineroRepository extends JpaRepository<MovimientoDinero, UUID> {
}
```

No tiene metodos propios porque con los heredados de `JpaRepository` alcanza para guardar movimientos.

## Configuracion

### `application.yml`

Archivo:

```text
src/main/resources/application.yml
```

Configuracion principal:

```yaml
spring:
  application:
    name: tpi-portfolio-service
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
  port: 8082

clients:
  history:
    base-url: ${HISTORY_BASE_URL:http://history-service:8084}
```

Que significa:

- `spring.application.name`: nombre del microservicio.
- `datasource.url`: URL de la base PostgreSQL.
- `datasource.username`: usuario de la base.
- `datasource.password`: password de la base.
- `ddl-auto: validate`: Hibernate valida que las tablas existan y coincidan, pero no las crea automaticamente.
- `server.port: 8082`: este microservicio escucha en el puerto `8082`.
- `clients.history.base-url`: URL del microservicio de historial.

### `RestClientsConfig`

```java
@Configuration
public class RestClientsConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
```

Esta clase crea un bean de `RestTemplate`.

`RestTemplate` sirve para hacer llamadas HTTP desde este microservicio hacia otro.

En este caso se usa para llamar a:

```text
history-service
```

## Manejo de errores

El archivo `GlobalExceptionHandler.java` centraliza errores.

### Errores controlados

```java
@ExceptionHandler(ResponseStatusException.class)
```

Captura errores lanzados a proposito desde el servicio.

Ejemplos:

- saldo insuficiente.
- cantidad insuficiente para vender.
- monto de deposito invalido.

Devuelve una respuesta JSON con:

```json
{
  "code": 400,
  "message": "Saldo insuficiente para realizar la compra.",
  "timestamp": "2026-06-28T20:00:00"
}
```

### Errores inesperados

```java
@ExceptionHandler(Exception.class)
```

Captura cualquier error no previsto y devuelve:

```json
{
  "code": 500,
  "message": "Error interno del servidor",
  "timestamp": "2026-06-28T20:00:00"
}
```

## Relacion con otros microservicios

### Con `orders-service`

`orders-service` puede llamar al endpoint:

```text
POST /users/{userId}/trades
```

Lo usa para modificar el portfolio despues de una compra o venta.

Por ejemplo:

- Si se ejecuta una compra, portfolio descuenta saldo y suma acciones.
- Si se ejecuta una venta, portfolio suma saldo y resta acciones.

### Con `history-service`

`portfolio-service` llama a:

```text
POST /events
```

del microservicio de historial cuando se carga saldo.

El evento enviado es:

```text
DEPOSIT_RECEIVED
```

Esto permite que el historial del usuario muestre tambien las cargas de saldo.

### Con `api-gateway`

El usuario normalmente no deberia llamar directo al puerto `8082`.

En el flujo general, las peticiones entran por el gateway:

```text
http://localhost:8085
```

Y el gateway las deriva al portfolio-service.

Ejemplo por gateway:

```text
http://localhost:8085/users/juan@example.com/portfolio
```

## Flujo completo de carga de saldo

1. El cliente llama al endpoint:

```text
POST /users/juan@example.com/portfolio/balance
```

2. `PortafolioController` recibe el request.

3. El controller llama a:

```java
portafolioService.deposito(userId, request)
```

4. `PortafolioService` busca o crea la cuenta.

5. Valida que el monto sea positivo.

6. Suma el monto al saldo.

7. Guarda un movimiento en `cash_movements`.

8. Envia un evento al history-service.

9. Devuelve el nuevo saldo.

## Flujo completo de compra

1. Llega un trade con `side = BUY`.

2. El servicio calcula:

```java
totalAmount = priceArs * quantity
```

3. Verifica que el saldo alcance.

4. Resta el total del saldo.

5. Busca la posicion por simbolo.

6. Si no existe, crea una posicion nueva.

7. Suma la cantidad comprada.

8. Recalcula el precio promedio.

9. Guarda cuenta y posicion.

10. Devuelve el portfolio actualizado.

## Flujo completo de venta

1. Llega un trade con `side = SELL`.

2. El servicio calcula el total de la venta.

3. Busca la posicion del simbolo.

4. Verifica que el usuario tenga acciones suficientes.

5. Resta la cantidad vendida.

6. Suma el dinero al saldo.

7. Si la posicion queda en cero, la elimina.

8. Si todavia quedan acciones, la actualiza.

9. Devuelve el portfolio actualizado.

## Tests existentes

El archivo de test es:

```text
src/test/java/com/tpi/portfolio/PortafolioServiceTest.java
```

Tests principales:

- `shouldCreateCuentaOnFirstPortfolioFetch`: verifica que al consultar un portfolio nuevo se cree una cuenta con saldo cero.
- `shouldDepositoFunds`: verifica que se pueda depositar saldo usando `/deposits`.
- `shouldLoadBalanceFromPortfolioEndpoint`: verifica que se pueda cargar saldo usando `/portfolio/balance`.

Los tests usan:

- `@SpringBootTest`: levanta el contexto de Spring.
- `@AutoConfigureMockMvc`: permite probar endpoints HTTP sin levantar un servidor real.
- `MockMvc`: simula requests HTTP.
- Repositories reales contra base de test.

Antes de cada test se limpia la base:

```java
@BeforeEach
void cleanDatabase() {
    movimientoDineroRepository.deleteAll();
    posicionRepository.deleteAll();
    cuentaRepository.deleteAll();
}
```

Esto evita que un test afecte al siguiente.

## Resumen para explicar en una exposicion

El microservicio `portfolio-service` administra el saldo y las acciones de cada usuario. Cuando alguien consulta su portfolio, el servicio busca su cuenta y sus posiciones; si la cuenta no existe, la crea automaticamente con saldo cero. Cuando se carga saldo, suma el dinero, registra un movimiento y avisa al microservicio de historial. Cuando se aplica una compra o venta, actualiza el saldo y las posiciones segun corresponda. La persistencia se hace con JPA sobre PostgreSQL y los endpoints se exponen con Spring Boot.
