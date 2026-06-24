# Comentarios Swagger/OpenAPI sugeridos (sin modificar código)

Este documento contiene bloques de anotaciones sugeridas para agregar manualmente en los controladores si en algún momento querés integrar `springdoc-openapi` o una generación automática desde el código.

> Importante: **este archivo no modifica el código existente**. Son sugerencias listas para copiar/pegar.

---

## Dependencias sugeridas si querés generar Swagger UI automáticamente

### Para microservicios Spring MVC
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

### Para el gateway Spring WebFlux
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

---

## 1) `tpi-market-data-service` - `CotizacionController`

Archivo: `tpi-market-data-service/src/main/java/com/tpi/marketdata/controller/CotizacionController.java`

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Market Data", description = "Consulta pública de cotizaciones")
@RestController
@RequestMapping("/quotes")
public class CotizacionController {

    @Operation(
        summary = "Obtener cotización por símbolo",
        description = "Consulta la cotización de mercado de un símbolo y devuelve su conversión a ARS."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cotización obtenida correctamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = CotizacionResponse.class),
                examples = @ExampleObject(value = """
                {
                  "simbolo": "AAPL",
                  "precio": 124000.00,
                  "compañia": "ARS",
                  "moneda": "YAHOO_FINANCE",
                  "marcaTiempo": "2026-06-23T18:45:30Z"
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Símbolo inválido"),
        @ApiResponse(responseCode = "500", description = "Error interno"),
        @ApiResponse(responseCode = "503", description = "Proveedor externo no disponible")
    })
    @GetMapping("/{simbolo}")
    public CotizacionResponse getCotizacion(
        @Parameter(description = "Ticker bursátil", example = "AAPL")
        @PathVariable String simbolo
    ) {
        return cotizacionMercadoService.getCotizacion(simbolo);
    }
}
```

---

## 2) `tpi-portfolio-service` - `PortafolioController`

Archivo: `tpi-portfolio-service/src/main/java/com/tpi/portfolio/controller/PortafolioController.java`

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Portfolio", description = "Operaciones sobre portafolios, depósitos y trades")
@RestController
@RequestMapping("/users/{userId}")
public class PortafolioController {

    @Operation(
        summary = "Consultar portafolio",
        description = "Obtiene el portafolio del usuario. Si no existe, puede crearse automáticamente según la lógica actual."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Portafolio obtenido correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/portfolio")
    public PortafolioResponse getPortafolio(
        @Parameter(description = "Identificador del usuario", example = "juan@example.com")
        @PathVariable String userId
    ) {
        return portafolioService.getPortafolio(userId);
    }

    @Operation(
        summary = "Registrar depósito",
        description = "Acredita saldo en ARS al usuario y registra el movimiento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Depósito registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Monto inválido"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping("/deposits")
    public DepositoResponse deposito(
        @Parameter(description = "Identificador del usuario", example = "juan@example.com")
        @PathVariable String userId,
        @Valid @org.springframework.web.bind.annotation.RequestBody DepositoRequest request
    ) {
        return portafolioService.deposito(userId, request);
    }

    @Operation(
        summary = "Aplicar trade al portafolio",
        description = "Aplica una compra o venta directamente sobre el portafolio del usuario."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Trade aplicado correctamente"),
        @ApiResponse(responseCode = "400", description = "Saldo insuficiente o posición inválida"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping("/trades")
    public PortafolioResponse applyTrade(
        @Parameter(description = "Identificador del usuario", example = "juan@example.com")
        @PathVariable String userId,
        @Valid @org.springframework.web.bind.annotation.RequestBody TradeAdjustmentRequest request
    ) {
        return portafolioService.aplicarTrade(userId, request);
    }
}
```

### Ejemplo recomendado para `DepositoRequest`
```java
@Schema(name = "DepositoRequest", description = "Solicitud para acreditar un depósito en ARS")
public record DepositoRequest(
    @Schema(description = "Monto a depositar en ARS", example = "25000.00", minimum = "0.01")
    @NotNull @DecimalMin(value = "0.01") BigDecimal amountArs,

    @Schema(description = "Referencia externa o idempotency key del depósito", example = "DEP-20260623-001")
    @NotBlank String referenceId
) {}
```

### Ejemplo recomendado para `TradeAdjustmentRequest`
```java
@Schema(name = "TradeAdjustmentRequest", description = "Ajuste de compra o venta aplicado al portafolio")
public record TradeAdjustmentRequest(
    @Schema(description = "Lado del trade", example = "BUY", allowableValues = {"BUY", "SELL"})
    @NotBlank String side,

    @Schema(description = "Símbolo bursátil", example = "META")
    @NotBlank String symbol,

    @Schema(description = "Cantidad negociada", example = "10.0000", minimum = "0.0001")
    @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,

    @Schema(description = "Precio en ARS", example = "335.0000", minimum = "0.01")
    @NotNull @DecimalMin(value = "0.01") BigDecimal priceArs,

    @Schema(description = "Referencia del trade u orden asociada", example = "ORDER-7b24d38d")
    @NotBlank String referenceId
) {}
```

---

## 3) `tpi-orders-service` - `OrdenController`

Archivo: `tpi-orders-service/src/main/java/com/tpi/orders/controller/OrdenController.java`

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Orders", description = "Alta y consulta de órdenes")
@RestController
@RequestMapping("/orders")
public class OrdenController {

    @Operation(
        summary = "Crear orden",
        description = "Registra una orden BUY o SELL y ejecuta las validaciones de negocio asociadas.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden creada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, saldo insuficiente o tenencias insuficientes"),
        @ApiResponse(responseCode = "404", description = "Portafolio no encontrado"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping
    public OrdenResponse createOrden(@Valid @RequestBody OrdenRequest request) {
        return ordenService.createOrden(request);
    }

    @Operation(
        summary = "Listar órdenes de un usuario",
        description = "Obtiene las órdenes del usuario ordenadas por fecha descendente.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Órdenes obtenidas correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/user/{userId}")
    public List<OrdenResponse> getOrdenes(
        @Parameter(description = "Identificador del usuario", example = "juan@example.com")
        @PathVariable String userId
    ) {
        return ordenService.getOrdenesByUsuario(userId);
    }
}
```

### Ejemplo recomendado para `OrdenRequest`
```java
@Schema(name = "OrdenRequest", description = "Solicitud de alta de orden")
public record OrdenRequest(
    @Schema(description = "Usuario que crea la orden", example = "juan@example.com")
    @NotBlank String userId,

    @Schema(description = "Símbolo bursátil", example = "META")
    @NotBlank String symbol,

    @Schema(description = "Tipo de orden", example = "BUY", allowableValues = {"BUY", "SELL"})
    @NotBlank
    @Pattern(regexp = "BUY|SELL")
    String side,

    @Schema(description = "Cantidad solicitada", example = "10")
    @NotNull @Positive BigDecimal quantity,

    @Schema(description = "Precio límite en ARS", example = "335.00")
    @NotNull @Positive BigDecimal limitPrice
) {}
```

---

## 4) `tpi-history-service` - `HistorialController`

Archivo: `tpi-history-service/src/main/java/com/tpi/history/controller/HistorialController.java`

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "History", description = "Auditoría e historial de eventos")
@RestController
@RequestMapping
public class HistorialController {

    @Operation(
        summary = "Registrar evento de historial",
        description = "Persiste un evento auditable y actualiza el modelo de lectura."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Evento registrado correctamente"),
        @ApiResponse(responseCode = "400", description = "Payload inválido"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @PostMapping("/events")
    public HistorialEventoResponse registraEvento(@Valid @RequestBody HistorialEventoRequest request) {
        return historialService.registraEvento(request);
    }

    @Operation(
        summary = "Consultar historial por usuario",
        description = "Devuelve los eventos de un usuario del más reciente al más antiguo.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial obtenido correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/users/{userId}/history")
    public List<HistorialEventoResponse> getHistorialUsuario(
        @Parameter(description = "Identificador del usuario", example = "juan@example.com")
        @PathVariable String userId
    ) {
        return historialService.getHistorialUsuario(userId);
    }

    @Operation(
        summary = "Consultar historial global",
        description = "Devuelve el historial completo del sistema.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial global obtenido correctamente"),
        @ApiResponse(responseCode = "403", description = "Acceso restringido a administradores"),
        @ApiResponse(responseCode = "500", description = "Error interno")
    })
    @GetMapping("/admin/history")
    public List<HistorialEventoResponse> getHistorialCompleto() {
        return historialService.getHistorialCompleto();
    }
}
```

---

## 5) `tpi-api-gateway` - `KeycloakAuthController`

Archivo: `tpi-api-gateway/src/main/java/com/tpi/gateway/controller/KeycloakAuthController.java`

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Gateway Auth", description = "Autenticación OAuth2/JWT y diagnóstico de tokens")
@RestController
@RequestMapping("/api/login/oauth2")
public class KeycloakAuthController {

    @Operation(
        summary = "Intercambiar authorization code por token",
        description = "Recibe un authorization code de Keycloak y devuelve el payload del token emitido."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token obtenido correctamente"),
        @ApiResponse(responseCode = "502", description = "No se pudo intercambiar el code por token")
    })
    @GetMapping("/code/keycloak")
    public Mono<ResponseEntity<String>> intercambiarCode(
        @Parameter(description = "Authorization code emitido por Keycloak")
        @RequestParam String code
    ) {
        return ...;
    }

    @Operation(
        summary = "Obtener datos del usuario autenticado",
        description = "Devuelve claims y authorities del JWT autenticado.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos del usuario autenticado"),
        @ApiResponse(responseCode = "401", description = "Token inválido o ausente")
    })
    @GetMapping("/me")
    public Mono<Map<String, Object>> me(...) {
        return ...;
    }

    @Operation(
        summary = "Depurar token manualmente",
        description = "Valida un JWT recibido por query string y devuelve su contenido decodificado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token válido"),
        @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    @GetMapping("/debug-token")
    public Mono<ResponseEntity<Map<String, Object>>> debugToken(
        @Parameter(description = "JWT a validar")
        @RequestParam String token
    ) {
        return ...;
    }
}
```

---

## 6) Configuración global sugerida OpenAPI

Podés centralizar título, versión, seguridad y descripción con una clase como esta:

```java
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TPI Backend 2026")
                .version("1.0.0")
                .description("Documentación OpenAPI consolidada de los microservicios"))
            .schemaRequirement("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
```

---

## Buenas prácticas recomendadas

1. **Documentar en el controlador y no solo en Postman**.
2. **Agregar `@Schema` en todos los DTOs** con ejemplos reales.
3. **Usar `@Operation` y `@ApiResponses`** en cada endpoint.
4. **Declarar seguridad por endpoint** con `@SecurityRequirement(name = "bearerAuth")`.
5. **Versionar rutas** cuando hagan cambios incompatibles, por ejemplo `/api/v1/orders`.
6. **Mantener consistencia entre gateway, controladores y colección Postman**.
7. **No usar query string para tokens en producción** salvo para debugging interno.
8. **Agregar ejemplos de error** además de ejemplos exitosos.
9. **Documentar discrepancias conocidas** hasta corregirlas en código.
10. **Validar la spec en CI** con una herramienta como `swagger-cli` o `openapi-generator-cli validate`.

---

## Hallazgos importantes del análisis

- El workspace real **no coincide** con los nombres que mencionaste (`user-service`, `payment-service`, `common-libs`, `api-gateway`). En este repo existen:
  - `tpi-api-gateway`
  - `tpi-market-data-service`
  - `tpi-portfolio-service`
  - `tpi-orders-service`
  - `tpi-history-service`
- No se encontró integración actual con `springdoc-openapi` en los `pom.xml`.
- Hay discrepancias entre rutas del gateway y controladores reales, especialmente en:
  - trades del portfolio
  - consulta de órdenes por usuario
- La colección Postman también muestra rutas distintas a algunas rutas reales del código.

Por eso la especificación OpenAPI consolidada se generó **reflejando fielmente el código actual** y dejando notas donde hay inconsistencias.

