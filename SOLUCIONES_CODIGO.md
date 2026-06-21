# 🔧 SOLUCIONES CONCRETAS - Código para Implementar

## 1. RESOLVER MERGE CONFLICT EN HistoryEvent.java

**Archivo:** `tpi-history-service/src/main/java/com/tpi/history/entity/HistoryEvent.java`

### Paso 1: Abrir el archivo en conflicto

El archivo actualmente tiene:
```java
<<<<<<< HEAD
    // to-do cambiar mapeo de Entidad a Bda relacion 
=======
    /*
    @JdbcTypeCode(SqlTypes.JSON), le estás diciendo a Hibernate:
    ...
    */
>>>>>>> 69e6349665b8c1546cf65a494a4021b69039f32f
```

### Paso 2: Elegir la solución correcta

**OPCIÓN A - Usar la rama actual (HEAD) - RECOMENDADO:**
```java
// Eliminar TODOS los marcadores y quedarse solo con el código limpio
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "payload_json", nullable = false)
private Map<String, Object> payloadJson;
```

**OPCIÓN B - Git resolve:**
```bash
cd c:\Users\juanc\Desktop\TpBack\TP_Backend
git checkout --ours tpi-history-service/src/main/java/com/tpi/history/entity/HistoryEvent.java
git add tpi-history-service/src/main/java/com/tpi/history/entity/HistoryEvent.java
git commit -m "Resolver conflicto de merge en HistoryEvent"
```

---

## 2. AGREGAR LOMBOK A TODOS LOS SERVICIOS

### 2.1 tpi-history-service/pom.xml
```xml
<!-- Agregar esta dependencia dentro de <dependencies> -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### 2.2 tpi-orders-service/pom.xml
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### 2.3 tpi-market-data-service/pom.xml
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### 2.4 Verificar compilación
```bash
mvn clean compile
```

---

## 3. LIMPIAR GETTERS/SETTERS EN HistoryEvent.java

**REEMPLAZAR TODA ESTA SECCIÓN:**
```java
// ❌ ELIMINAR TODO ESTO (getters y setters manuales)
public UUID getEventId() {
    return eventId;
}

public void setEventId(UUID eventId) {
    this.eventId = eventId;
}

public String getEventType() {
    return eventType;
}

public void setEventType(String eventType) {
    this.eventType = eventType;
}
// ... todos los demás getters/setters
```

**CON ESTO:**
```java
// ✅ Lombok genera todo automáticamente
// No necesita nada más en la clase
```

El archivo debe quedar así al final:
```java
package com.tpi.history.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "history_events", schema = "history")
public class HistoryEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "causation_id")
    private UUID causationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false)
    private Map<String, Object> payloadJson;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = LocalDateTime.now();
        }
    }
}
```

---

## 4. AGREGAR LOGGING CON LOMBOK EN OrdersService

**Archivo:** `tpi-orders-service/src/main/java/com/tpi/orders/service/OrdersService.java`

### Cambio 1: Agregar anotación @Slf4j
```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j  // ← Agregar esto
public class OrdersService {
    // ... resto del código
}
```

### Cambio 2: Agregar logs en el método buy()
```java
@Transactional
public OrderResponse buy(OrderRequest request) {
    log.info("▶️ Procesando orden de compra: userId={}, symbol={}, qty={}, priceLimit={}", 
             request.userId(), request.symbol(), request.quantity(), request.priceLimit());
    
    try {
        // Consultar cotización
        var marketQuote = fetchQuote(request.symbol());
        log.debug("✓ Cotización obtenida: symbol={}, price={}, currency={}", 
                 marketQuote.symbol(), marketQuote.price(), marketQuote.currency());
        
        var order = createOrder(request, "BUY");
        log.debug("✓ Orden creada: orderId={}", order.getId());
        
        // Validar balance
        validateBuyBalance(order, marketQuote.price());
        log.debug("✓ Balance validado: requiredBalance={} ARS", 
                 order.getQuantity().multiply(order.getLimitPrice()));
        
        ordenRepository.save(order);
        log.debug("✓ Orden persistida en BD");

        // Matching
        var availableSellers = ordenRepository.findMatchingSellers(order.getSymbol(), "SELL");
        log.debug("✓ Encontradas {} órdenes de venta disponibles", availableSellers.size());
        
        var matchedQuantity = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        for (var seller : availableSellers) {
            if (order.getRemainingQuantity().compareTo(BigDecimal.ZERO) == 0) {
                log.debug("✓ Cantidad completa satisfecha, saliendo del loop");
                break;
            }
            
            if (seller.getUserId().equals(order.getUserId())) {
                log.debug("⊘ Saltando venta del mismo usuario: userId={}", order.getUserId());
                continue;
            }
            
            if (seller.getLimitPrice().compareTo(order.getLimitPrice()) > 0) {
                log.debug("⊘ Precio no coincide: seller.price={} > buyer.price={}", 
                         seller.getLimitPrice(), order.getLimitPrice());
                continue;
            }

            var fillQty = seller.getRemainingQuantity().min(order.getRemainingQuantity())
                    .setScale(4, RoundingMode.HALF_UP);
            
            if (fillQty.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("⊘ Cantidad inválida: fillQty={}", fillQty);
                continue;
            }
            
            log.info("✓ MATCH encontrado: buyer={}, seller={}, qty={}, price={}", 
                    order.getUserId(), seller.getUserId(), fillQty, seller.getLimitPrice());
            
            var executionPrice = seller.getLimitPrice().setScale(4, RoundingMode.HALF_UP);
            
            applyTradeToPortfolio(order.getUserId(), "BUY", order.getSymbol(), fillQty, 
                                 executionPrice, order.getId().toString());
            applyTradeToPortfolio(seller.getUserId(), "SELL", seller.getSymbol(), fillQty, 
                                 executionPrice, seller.getId().toString());

            seller.setRemainingQuantity(seller.getRemainingQuantity().subtract(fillQty));
            seller.setStatus(resolveStatus(seller.getRemainingQuantity(), seller.getQuantity()));
            order.setRemainingQuantity(order.getRemainingQuantity().subtract(fillQty));
            order.setStatus(resolveStatus(order.getRemainingQuantity(), order.getQuantity()));
            
            matchedQuantity = matchedQuantity.add(fillQty);
        }

        ordenRepository.saveAll(List.of(order));
        recordHistory("BUY_ORDER_EXECUTED", order, matchedQuantity, order.getRemainingQuantity(), 
                     order.getLimitPrice(), "Orden de compra ejecutada");
        
        log.info("✅ Orden completada: orderId={}, matchedQty={}, remainingQty={}, status={}", 
                order.getId(), matchedQuantity, order.getRemainingQuantity(), order.getStatus());
        
        return toResponse(order, matchedQuantity, order.getRemainingQuantity(), 
                         order.getLimitPrice(), "Orden de compra ejecutada");
    } catch (Exception e) {
        log.error("❌ Error procesando orden de compra: userId={}, symbol={}", 
                 request.userId(), request.symbol(), e);
        throw e;
    }
}
```

### Cambio 3: Lo mismo para sell()
```java
@Transactional
public OrderResponse sell(OrderRequest request) {
    log.info("▶️ Procesando orden de venta: userId={}, symbol={}, qty={}, priceLimit={}", 
             request.userId(), request.symbol(), request.quantity(), request.priceLimit());
    try {
        var order = createOrder(request, "SELL");
        validateSellAvailability(order);
        order.setStatus("PENDING");
        ordenRepository.save(order);
        
        log.info("✅ Orden de venta registrada: orderId={}, status={}", 
                order.getId(), order.getStatus());
        
        recordHistory("SELL_ORDER_CREATED", order, order.getQuantity(), 
                     order.getRemainingQuantity(), order.getLimitPrice(), 
                     "Orden de venta registrada");
        
        return toResponse(order, BigDecimal.ZERO, order.getRemainingQuantity(), 
                         order.getLimitPrice(), "Orden de venta registrada");
    } catch (Exception e) {
        log.error("❌ Error procesando orden de venta: userId={}, symbol={}", 
                 request.userId(), request.symbol(), e);
        throw e;
    }
}
```

---

## 5. AGREGAR VALIDACIONES EN OrdersService

### Nuevo método de validación:
```java
/**
 * Valida que el usuario tenga saldo suficiente para la compra
 */
private void validateBuyBalance(OrderEntity order, BigDecimal marketPrice) {
    log.debug("Validando balance para compra: userId={}, qty={}, limitPrice={}", 
             order.getUserId(), order.getQuantity(), order.getLimitPrice());
    
    // Consultar portafolio del usuario
    try {
        PortfolioClientResponse portfolio = restTemplate.getForObject(
            portfolioBaseUrl + "/users/" + order.getUserId() + "/portfolio",
            PortfolioClientResponse.class
        );
        
        if (portfolio == null) {
            log.warn("⚠️ Portfolio no encontrado para userId: {}", order.getUserId());
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Portfolio no encontrado para el usuario"
            );
        }
        
        // Calcular monto requerido
        BigDecimal requiredAmount = order.getQuantity()
            .multiply(order.getLimitPrice())
            .setScale(2, RoundingMode.HALF_UP);
        
        log.debug("Balance requerido: {} ARS, Balance disponible: {} ARS", 
                 requiredAmount, portfolio.balanceArs());
        
        // Validar saldo
        if (portfolio.balanceArs().compareTo(requiredAmount) < 0) {
            log.warn("❌ Saldo insuficiente: usuario={}, requerido={}, disponible={}", 
                    order.getUserId(), requiredAmount, portfolio.balanceArs());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "Saldo insuficiente. Requerido: %s ARS, Disponible: %s ARS",
                    requiredAmount, portfolio.balanceArs()
                )
            );
        }
        
        log.debug("✓ Balance validado correctamente");
        
    } catch (Exception e) {
        log.error("Error validando balance: {}", e.getMessage(), e);
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error al validar saldo del usuario"
        );
    }
}

/**
 * Valida que el usuario tenga las acciones para vender
 */
private void validateSellAvailability(OrderEntity order) {
    log.debug("Validando disponibilidad de acciones para venta: userId={}, symbol={}, qty={}", 
             order.getUserId(), order.getSymbol(), order.getQuantity());
    
    try {
        PortfolioClientResponse portfolio = restTemplate.getForObject(
            portfolioBaseUrl + "/users/" + order.getUserId() + "/portfolio",
            PortfolioClientResponse.class
        );
        
        if (portfolio == null) {
            log.warn("⚠️ Portfolio no encontrado para userId: {}", order.getUserId());
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Portfolio no encontrado para el usuario"
            );
        }
        
        // Buscar la posición del símbolo
        var position = portfolio.positions().stream()
            .filter(p -> p.symbol().equalsIgnoreCase(order.getSymbol()))
            .findFirst()
            .orElse(null);
        
        if (position == null || position.quantity().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("❌ Posición no disponible para venta: usuario={}, symbol={}", 
                    order.getUserId(), order.getSymbol());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No tiene acciones de " + order.getSymbol() + " para vender"
            );
        }
        
        if (position.quantity().compareTo(order.getQuantity()) < 0) {
            log.warn("❌ Cantidad insuficiente de acciones: usuario={}, symbol={}, " +
                    "requerida={}, disponible={}", 
                    order.getUserId(), order.getSymbol(), 
                    order.getQuantity(), position.quantity());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "Cantidad insuficiente de %s. Tiene: %s, quiere vender: %s",
                    order.getSymbol(), position.quantity(), order.getQuantity()
                )
            );
        }
        
        log.debug("✓ Disponibilidad validada: posee {} de {}", 
                 position.quantity(), order.getSymbol());
        
    } catch (ResponseStatusException e) {
        throw e;
    } catch (Exception e) {
        log.error("Error validando disponibilidad de acciones: {}", e.getMessage(), e);
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error al validar disponibilidad de acciones"
        );
    }
}
```

---

## 6. INTEGRAR API EXTERNA (Market Data)

### 6.1 Agregar dependencia a market-data-service/pom.xml
```xml
<!-- Opción A: Yahoo Finance -->
<dependency>
    <groupId>com.yahoofinance-api</groupId>
    <artifactId>YahooFinanceAPI</artifactId>
    <version>3.17.0</version>
</dependency>

<!-- Opción B: Finnhub (JSON) -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.10.0</version>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

### 6.2 Implementar con Yahoo Finance (RECOMENDADO)
```java
package com.tpi.marketdata.service;

import com.tpi.marketdata.dto.CotizacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class MarketQuoteService {

    // Precios por defecto si la API falla
    private static final Map<String, BigDecimal> FALLBACK_QUOTES = Map.of(
        "AAPL", new BigDecimal("27500.00"),
        "AMZN", new BigDecimal("18250.00"),
        "GOOGL", new BigDecimal("30110.00"),
        "MELI", new BigDecimal("65400.00"),
        "MSFT", new BigDecimal("31200.00"),
        "NFLX", new BigDecimal("28990.00"),
        "NVDA", new BigDecimal("36000.00"),
        "TSLA", new BigDecimal("24150.00")
    );

    public QuoteResponse getQuote(String symbol) {
        var normalized = normalize(symbol);
        
        try {
            log.debug("Consultando cotización en Yahoo Finance: {}", normalized);
            
            // Consultar a Yahoo Finance
            StockQuote quote = YahooFinance.get(normalized).getQuote();
            
            if (quote == null || quote.getPrice() == null) {
                log.warn("Cotización vacía en Yahoo Finance para: {}", normalized);
                return getQuoteFromFallback(normalized);
            }
            
            BigDecimal price = new BigDecimal(quote.getPrice().toString())
                .setScale(2, java.math.RoundingMode.HALF_UP);
            
            log.info("✓ Cotización obtenida: symbol={}, price={} USD", normalized, price);
            
            // OPCIONAL: Convertir a ARS si es necesario
            BigDecimal priceArs = price.multiply(new BigDecimal("800")); // Tipo de cambio aprox
            
            return new QuoteResponse(
                normalized,
                priceArs,  // En ARS como requiere el TP
                "ARS",     // Moneda
                "YAHOO_FINANCE",
                OffsetDateTime.now()
            );
            
        } catch (IOException e) {
            log.warn("⚠️ Error consultando Yahoo Finance para {}: {}", normalized, e.getMessage());
            return getQuoteFromFallback(normalized);
        }
    }

    private QuoteResponse getQuoteFromFallback(String symbol) {
        var price = FALLBACK_QUOTES.get(symbol);
        if (price == null) {
            log.error("❌ No existe cotización para símbolo: {}", symbol);
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe cotización para el símbolo: " + symbol
            );
        }
        log.info("⚠️ Usando cotización fallback: symbol={}, price={} ARS", symbol, price);
        return new QuoteResponse(
            symbol,
            price,
            "ARS",
            "FALLBACK_CATALOG",
            OffsetDateTime.now()
        );
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El símbolo es obligatorio"
            );
        }
        return symbol.trim().toUpperCase();
    }
}
```

### 6.3 Alternativa: Finnhub (Si prefieren JSON puro)
```java
package com.tpi.marketdata.service;

import com.tpi.marketdata.dto.CotizacionResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Slf4j
public class MarketQuoteService {

    private static final String FINNHUB_BASE_URL = "https://finnhub.io/api/v1/quote";
    
    @Value("${finnhub.api-key:NO_API_KEY}")
    private String finnhubApiKey;

    private static final Map<String, BigDecimal> FALLBACK_QUOTES = Map.of(
        "AAPL", new BigDecimal("27500.00"),
        "AMZN", new BigDecimal("18250.00"),
        // ... más símbolos
    );

    private final OkHttpClient client = new OkHttpClient();

    public QuoteResponse getQuote(String symbol) {
        var normalized = normalize(symbol);
        
        try {
            log.debug("Consultando cotización en Finnhub: {}", normalized);
            
            String url = FINNHUB_BASE_URL + "?symbol=" + normalized + "&token=" + finnhubApiKey;
            
            Request request = new Request.Builder()
                .url(url)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Finnhub retornó status {}", response.code());
                    return getQuoteFromFallback(normalized);
                }
                
                String body = response.body().string();
                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                
                if (!json.has("c")) {
                    log.warn("Respuesta inválida de Finnhub para {}", normalized);
                    return getQuoteFromFallback(normalized);
                }
                
                BigDecimal price = new BigDecimal(json.get("c").getAsDouble())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
                
                // Convertir a ARS
                BigDecimal priceArs = price.multiply(new BigDecimal("800"));
                
                log.info("✓ Cotización obtenida: symbol={}, price={} ARS", normalized, priceArs);
                
                return new QuoteResponse(
                    normalized,
                    priceArs,
                    "ARS",
                    "FINNHUB",
                    OffsetDateTime.now()
                );
            }
            
        } catch (IOException e) {
            log.warn("⚠️ Error consultando Finnhub para {}: {}", normalized, e.getMessage());
            return getQuoteFromFallback(normalized);
        }
    }

    private QuoteResponse getQuoteFromFallback(String symbol) {
        var price = FALLBACK_QUOTES.get(symbol);
        if (price == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No existe cotización para: " + symbol
            );
        }
        return new QuoteResponse(symbol, price, "ARS", "FALLBACK", OffsetDateTime.now());
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El símbolo es obligatorio"
            );
        }
        return symbol.trim().toUpperCase();
    }
}
```

---

## 7. AGREGAR GLOBAL EXCEPTION HANDLER

**Crear archivo:** `tpi-api-gateway/src/main/java/com/tpi/gateway/exception/GlobalExceptionHandler.java`

```java
package com.tpi.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException e) {
        log.warn("ResponseStatusException: {} - {}", e.getStatusCode(), e.getReason());
        
        return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(
                    e.getStatusCode().value(),
                    e.getReason() != null ? e.getReason() : "Error en la solicitud",
                    OffsetDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Excepción inesperada:", e);
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                    500,
                    "Error interno del servidor",
                    OffsetDateTime.now()
                ));
    }
}

record ErrorResponse(
    int code,
    String message,
    OffsetDateTime timestamp
) {}
```

Hacer lo mismo en cada servicio con sus DTOs específicos.

---

## 8. AGREGAR SPRINGDOC-OPENAPI

### Paso 1: Agregar dependencia a TODOS los pom.xml
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>
```

### Paso 2: Agregar a cada controller
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/quotes")
@Tag(name = "Cotizaciones", description = "API de consulta de cotizaciones (Público)")
public class QuoteController {

    @GetMapping("/{symbol}")
    @Operation(
        summary = "Obtener cotización",
        description = "Retorna la cotización actual de una acción en ARS"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cotización encontrada"),
        @ApiResponse(responseCode = "400", description = "Símbolo inválido"),
        @ApiResponse(responseCode = "404", description = "Símbolo no encontrado")
    })
    public QuoteResponse getQuote(@PathVariable String symbol) {
        return marketQuoteService.getQuote(symbol);
    }
}
```

### Paso 3: Acceder a Swagger
- URL: `http://localhost:8085/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8085/v3/api-docs`

---

## CHECKLIST DE IMPLEMENTACIÓN

```
✅ Merge conflict resuelto
✅ Lombok agregado a todos los pom.xml
✅ Getters/setters eliminados
✅ Logging agregado con @Slf4j
✅ Validaciones implementadas
✅ API externa integrada
✅ Exception handlers globales
✅ OpenAPI documentado
✅ Compilación sin errores: mvn clean install
✅ Tests pasan: mvn test
✅ Docker compose levanta: make up
✅ Postman collection actualizada
```

