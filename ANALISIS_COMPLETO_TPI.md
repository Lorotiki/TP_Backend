# 📊 ANÁLISIS COMPLETO - Proyecto TPI Backend 2026

**Fecha:** 20/06/2026  
**Estado General:** ⚠️ **70% implementado - Hay problemas críticos por resolver**

---

## 📋 TABLA DE CONTENIDOS
1. [Cumplimiento de Requerimientos](#cumplimiento-de-requerimientos)
2. [Problemas Críticos Identificados](#problemas-críticos-identificados)
3. [Mejoras Técnicas Recomendadas](#mejoras-técnicas-recomendadas)
4. [Análisis por Servicio](#análisis-por-servicio)
5. [Plan de Acción Inmediato](#plan-de-acción-inmediato)

---

## ✅ CUMPLIMIENTO DE REQUERIMIENTOS

### Requerimientos Funcionales

| Requerimiento | Estado | Observaciones |
|---|---|---|
| 1. Consultar cotizaciones | ✅ Implementado | `/quotes/{symbol}` - Endpoint público sin autenticación |
| 2. Consultar portfolio y saldo | ✅ Implementado | `/users/{userId}/portfolio` - Requiere autenticación |
| 3. Registrar órdenes de compra | ✅ Parcial | `/orders/buy` existe pero falta validación completa |
| 4. Registrar órdenes de venta | ✅ Parcial | `/orders/sell` existe pero falta validación |
| 5. Motor de matching | ⚠️ Incompleto | Tiene lógica básica pero con errores |
| 6. Historial por usuario | ✅ Implementado | `/users/{userId}/history` funciona |
| 7. Historial ADMIN | ✅ Implementado | `/admin/history` con rol ADMIN |

### Requerimientos de Seguridad

| Aspecto | Estado | Detalles |
|---|---|---|
| OAuth2 + Keycloak | ✅ Configurado | SecurityConfig con JWT validado |
| Autenticación | ✅ Implementado | Gateway valida tokens con Keycloak |
| Roles (USER/ADMIN) | ✅ Implementado | Configurados en realm |
| Endpoint público | ✅ Correcto | `/quotes/**` permitido sin token |
| Endpoints privados | ✅ Protegidos | Requieren autenticación |

### Requerimientos de Arquitectura

| Aspecto | Estado | Detalles |
|---|---|---|
| Microservicios | ✅ Implementado | 5 servicios + API Gateway |
| Único punto entrada | ✅ API Gateway | `tpi-api-gateway` en puerto 8085 |
| Docker Compose | ✅ Disponible | `tpi-platform` con compose configurado |
| BD por servicio | ✅ Implementado | Portfolio (5433), Orders (5434), History (5435) |
| Routing | ✅ Configurado | Cloud Gateway con rutas definidas |

---

## 🔴 PROBLEMAS CRÍTICOS IDENTIFICADOS

### 1. **CONFLICTO DE MERGE NO RESUELTO** 🚨

**Archivo:** `tpi-history-service/src/main/java/com/tpi/history/entity/HistoryEvent.java`

**Problema:** El archivo tiene marcadores de conflicto Git sin resolver:
```
<<<<<<< HEAD
    // to-do cambiar mapeo de Entidad a Bda relacion 
=======
    /*
@JdbcTypeCode(SqlTypes.JSON), le estás diciendo a Hibernate:
...
>>>>>>> 69e6349665b8c1546cf65a494a4021b69039f32f
```

**Impacto:** Crítico - El proyecto **NO compila** con esto.

**Solución:** Resolver merge inmediatamente eligiendo una rama.

---

### 2. **LOMBOK NO FUNCIONA CORRECTAMENTE** 🔧

**Problema:** `HistoryEvent` usa `@Data` pero sigue teniendo 200+ líneas de getters/setters manuales:
```java
@Data  // ← Esto debería generar getters/setters automáticamente
@AllArgsConstructor
@NoArgsConstructor
public class HistoryEvent {
    // Pero luego tiene:
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    // ... 50+ métodos manuales más
```

**Causas:**
- Lombok **no está en el POM de history-service ni orders-service**
- La anotación `@Data` no se procesa sin la dependencia

**Impacto:** 
- Código muy verboso
- Difícil de mantener
- Redundancia total

**Solución:** Agregar Lombok a TODOS los servicios y limpiar getters/setters.

---

### 3. **API EXTERNA NO IMPLEMENTADA** 🌐

**Archivo:** `tpi-market-data-service/src/main/java/com/tpi/marketdata/service/MarketQuoteService.java`

**Problema:** Tiene comentario TODO explícito:
```java
// to-do reemplazar por integración con proveedor de datos real, 
// o al menos un repositorio local con datos históricos
private static final Map<String, BigDecimal> QUOTES = Map.of(
    "AAPL", new BigDecimal("27500.00"),
    "AMZN", new BigDecimal("18250.00"),
    // ... datos hardcodeados
);
```

**Requerimiento:** El enunciado pide **"Pueden usar el API que quieran: Yahoo Finance o cualquier otra"**

**Impacto:** 
- Las cotizaciones están **ficticias**
- No hay conversión de monedas real
- No cumple con el requerimiento de API externa

**Solución:** Integrar API real (Yahoo Finance, Alpha Vantage, Finnhub, etc.)

---

### 4. **VALIDACIÓN DE ÓRDENES INCOMPLETA** ⚠️

**Archivo:** `tpi-orders-service/src/main/java/com/tpi/orders/service/OrdersService.java`

**Problemas:**
- El `buy()` consulta cotización actual pero **no valida si el usuario tiene saldo suficiente en ARS**
- El `sell()` **no verifica si el usuario tiene las acciones para vender**
- No hay manejo de excepciones robusto
- Falta validación de entrada (symbol null, quantities negativas, etc.)

**Ejemplo del Issue:**
```java
public OrderResponse buy(OrderRequest request) {
    var marketQuote = fetchQuote(request.symbol());  // ¿Y si falla?
    var order = createOrder(request, "BUY");
    validateBuyBalance(order, marketQuote.price());  // ¿Qué pasa si falla?
    // ... Sin try-catch
}
```

**Impacto:** Posibles transacciones inválidas, errores en runtime.

---

### 5. **CONVERSIÓN DE MONEDAS NO IMPLEMENTADA** 💱

**Problema:** El enunciado menciona:
> "Tengan especial atención a la posible necesidad de realizar conversiones de monedas"

**Estado:** 
- Todo está en ARS (está bien)
- Pero NO hay implementación para si alguna acción estuviera en otra moneda
- No hay servicio de conversión

**Impacto:** No es requisito obligatorio pero está incompleto.

---

### 6. **TESTING VACÍO O MÍNIMO** 🧪

**Estado:** 
- Hay algunos test en `target/surefire-reports/` pero muy pocos
- Los servicios tienen estructura pero poco testeo unitario
- Sin integration tests

**Impacto:** Riesgo de regresiones, calidad comprometida.

---

### 7. **DOCUMENTACIÓN DE API** 📚

**Problema:** 
- NO hay Swagger/OpenAPI documentado
- La colección Postman que mencionan en requerimientos **no está en el repo**
- Falta documentación de DTOs

**Impacto:** Dificulta testing y demostración.

---

### 8. **LOGGING Y OBSERVABILIDAD** 📊

**Problema:** 
- NO hay logger implementado (ni `@Slf4j`)
- NO hay métricas (Micrometer/Actuator está pero no usado)
- NO hay structured logging

**Impacto:** 
- Difícil debuggear en producción
- No cumple con "mejoras técnicas no cubiertas en clase" (vale 15% de nota)

---

### 9. **ERROR HANDLING DÉBIL** 🚨

**Problema:** 
- Faltan `@ExceptionHandler` globales
- Respuestas de error inconsistentes
- Falta validación con `@Valid`

**Ejemplo:**
```java
@PostMapping("/events")
public HistoryEventResponse registerEvent(@Valid @RequestBody HistoryEventRequest request) {
    // Sin try-catch, sin validación de negocio
    return historialService.registerEvent(request);
}
```

---

### 10. **CONFIGURACIÓN DE BASE DE DATOS** 🗄️

**Problema:** 
- `ddl-auto: none` pero **no hay scripts de inicialización claros**
- Falta schema.sql o Flyway/Liquibase
- Las tablas deben existir pero **¿cómo se crean?**

**Impacto:** Difícil reproducir entorno.

---

## 💡 MEJORAS TÉCNICAS RECOMENDADAS

### ALTA PRIORIDAD (Crítico)

#### 1. Resolver Merge Conflict ⭐
```bash
# En tpi-history-service, elegir una rama y limpiar
git status  # Ver el conflicto
# Editar el archivo y elegir la rama correcta
git add tpi-history-service/src/main/java/com/tpi/history/entity/HistoryEvent.java
git commit -m "Resolver conflicto de merge en HistoryEvent"
```

#### 2. Implementar Lombok en TODOS los servicios ⭐
```xml
<!-- Agregar a pom.xml de history-service, orders-service, market-data-service -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

Luego, limpiar todos los getters/setters manuales.

#### 3. Integrar API Externa Real ⭐

**Opción A: Yahoo Finance (Recomendado - Gratis)**
```java
// https://github.com/sstrickx/yahoofinance-api
<dependency>
    <groupId>com.yahoofinance-api</groupId>
    <artifactId>YahooFinanceAPI</artifactId>
    <version>3.17.0</version>
</dependency>
```

**Opción B: Finnhub (Alternativa)**
```java
// https://finnhub.io/docs/api
private static final String API_KEY = "${FINNHUB_API_KEY}";
private static final String BASE_URL = "https://finnhub.io/api/v1/quote?symbol=";
```

#### 4. Completar Validaciones en OrdersService ⭐

```java
@Transactional
public OrderResponse buy(OrderRequest request) {
    // 1. Validar entrada
    validateOrderRequest(request);
    
    // 2. Consultar cotización (con reintentos)
    QuoteClientResponse quote = fetchQuoteWithRetry(request.symbol());
    
    // 3. Validar saldo en ARS
    BigDecimal requiredBalance = request.quantity()
        .multiply(request.priceLimit())
        .setScale(2, RoundingMode.HALF_UP);
    
    Portfolio portfolio = portfolioService.getPortfolio(request.userId());
    if (portfolio.getBalanceArs().compareTo(requiredBalance) < 0) {
        throw new InsufficientBalanceException(
            "Balance insuficiente. Necesita: " + requiredBalance + " ARS"
        );
    }
    
    // 4. Crear orden y hacer matching...
}
```

---

### MEDIA PRIORIDAD (Importante)

#### 5. Implementar Logging Estructurado

```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j  // ← Automático con Lombok
public class OrdersService {
    
    @Transactional
    public OrderResponse buy(OrderRequest request) {
        log.info("Procesando orden de compra: userId={}, symbol={}, qty={}", 
                 request.userId(), request.symbol(), request.quantity());
        
        try {
            // ... lógica
            log.debug("Orden creada: orderId={}, status={}", orderId, status);
            return response;
        } catch (Exception e) {
            log.error("Error procesando orden: userId={}, symbol={}", 
                     request.userId(), request.symbol(), e);
            throw e;
        }
    }
}
```

#### 6. Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(
            InsufficientBalanceException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INSUFFICIENT_BALANCE", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message));
    }
}

@Data
@AllArgsConstructor
record ErrorResponse(String code, String message) {}
```

#### 7. Documentación API con Springdoc-OpenAPI

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>
```

```java
@RestController
@RequestMapping("/orders")
public class OrdersController {
    
    @PostMapping("/buy")
    @Operation(summary = "Crear orden de compra", 
               description = "Crea una orden de compra y busca matching inmediato")
    @ApiResponse(responseCode = "200", description = "Orden creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "No autenticado")
    public OrderResponse buy(@Valid @RequestBody OrderRequest request) {
        return ordenService.buy(request);
    }
}

// Accesible en http://localhost:8085/swagger-ui.html (via Gateway)
```

#### 8. Crear Colección Postman Oficial

**Crear archivo:** `postman_collection_tpi_backend_2026.json` (ya existe, verificar actualización)

Debe contener:
- `/quotes/{symbol}` - Test sin autenticación
- `/users/{userId}/portfolio` - Test con autenticación USER
- `/orders/buy` - Test transacción compra
- `/orders/sell` - Test transacción venta
- `/users/{userId}/history` - Test historial usuario
- `/admin/history` - Test historial ADMIN

#### 9. Flyway o Liquibase para Versionado de BD

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
    <version>9.22.1</version>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <version>9.22.1</version>
</dependency>
```

```yaml
# application.yml
spring:
  flyway:
    locations: classpath:db/migration
    baselineOnMigrate: true
```

Crear scripts:
```sql
-- src/main/resources/db/migration/V1__initial_schema.sql
CREATE SCHEMA IF NOT EXISTS portfolio;
CREATE TABLE portfolio.accounts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL,
    balance_ars DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
-- ... más tablas
```

---

### BAJA PRIORIDAD (Mejoras Opcionales)

#### 10. Circuit Breaker (Resilience4j)

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
</dependency>
```

```java
@Service
@Slf4j
public class OrdersService {
    
    @CircuitBreaker(name = "marketDataService")
    @Retry(name = "marketDataService")
    private QuoteClientResponse fetchQuoteWithRetry(String symbol) {
        log.debug("Consultando cotización: {}", symbol);
        return restTemplate.getForObject(
            marketDataBaseUrl + "/quotes/" + symbol,
            QuoteClientResponse.class
        );
    }
}
```

#### 11. Caché de Cotizaciones

```java
@Service
@Slf4j
public class MarketQuoteService {
    
    @Cacheable(value = "quotes", key = "#symbol", 
               unless = "#result == null",
               cacheManager = "cacheManager")
    public QuoteResponse getQuote(String symbol) {
        // Llamar a API externa
    }
    
    @CacheEvict(value = "quotes", key = "#symbol")
    public void evictQuote(String symbol) {
        log.info("Evicted cache for: {}", symbol);
    }
}

// application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=500,expireAfterWrite=5m
```

#### 12. Métricas con Micrometer

```java
@Service
@Slf4j
public class OrdersService {
    
    private final MeterRegistry meterRegistry;
    
    @Transactional
    public OrderResponse buy(OrderRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // ... lógica compra
            meterRegistry.counter("orders.buy.success").increment();
            sample.stop(Timer.builder("orders.buy.duration")
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry));
            return response;
        } catch (Exception e) {
            meterRegistry.counter("orders.buy.error", 
                                 "exception", e.getClass().getSimpleName()).increment();
            throw e;
        }
    }
}

// application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 13. Health Checks Custom

```java
@Component
public class DatabaseHealthCheck implements HealthIndicator {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Override
    public Health health() {
        try {
            long count = portfolioRepository.count();
            return Health.up()
                    .withDetail("accounts_count", count)
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }
}
```

---

## 📊 ANÁLISIS POR SERVICIO

### 🔷 tpi-api-gateway (Puerto 8085)

**Estado:** ✅ 90% Bien configurado

**Fortalezas:**
- ✅ SecurityConfig correcto con OAuth2/JWT
- ✅ Rutas bien definidas para cada servicio
- ✅ Endpoint `/quotes/**` público como requerimiento
- ✅ Actuator habilitado

**Mejoras:**
- ⚠️ Faltan global exception handlers
- ⚠️ Falta logging de requests/responses
- ⚠️ Sin rate limiting

**Acción:** Agregar logging y exception handling global.

---

### 🔷 tpi-market-data-service (Puerto 8081)

**Estado:** ⚠️ 50% Incompleto

**Problemas:**
- ❌ API externa NO integrada (datos hardcodeados)
- ❌ Sin conversión de monedas
- ✅ Endpoint `/quotes/{symbol}` funciona
- ⚠️ Sin validación robusta

**Acción Inmediata:** Integrar API real (Yahoo Finance o Finnhub).

**Estimación:** 2-3 horas

---

### 🔷 tpi-orders-service (Puerto 8083)

**Estado:** ⚠️ 60% Parcial

**Problemas:**
- ❌ NO validación de saldo antes de compra
- ❌ NO validación de acciones disponibles para venta
- ❌ Sin manejo robusto de errores
- ⚠️ Lógica de matching básica pero con bugs
- ❌ Sin Lombok (getters/setters redundantes)

**DTOs faltantes:**
- Necesita DTOs propios (ahora usa clientes)

**Acción Inmediata:** 
1. Agregar Lombok
2. Validar transacciones
3. Mejorar matching logic

**Estimación:** 4-5 horas

---

### 🔷 tpi-portfolio-service (Puerto 8082)

**Estado:** ⚠️ 70% Funcional

**Fortalezas:**
- ✅ Usa Lombok correctamente
- ✅ Estructura de entidades clara
- ✅ DTOs bien definidos

**Mejoras:**
- ⚠️ Falta validación de operaciones
- ⚠️ Sin logging
- ⚠️ Sin exception handler global

**Acción:** Agregar validaciones y logging.

---

### 🔷 tpi-history-service (Puerto 8084)

**Estado:** ❌ 40% Muy Problemático

**Problemas CRÍTICOS:**
- 🚨 **CONFLICTO DE MERGE SIN RESOLVER** (bloquea compilación)
- ❌ Lombok anotado pero NO en POM
- ❌ 200+ líneas de getters/setters que deberían ser automáticos
- ⚠️ Sin validación

**Acción Inmediata:**
1. Resolver merge conflict
2. Agregar Lombok a POM
3. Limpiar getters/setters

**Estimación:** 1-2 horas

---

## 🎯 PLAN DE ACCIÓN INMEDIATO

### FASE 1: CRÍTICO (2-3 horas)

```
1. [ ] Resolver merge conflict en HistoryEvent.java
2. [ ] Agregar Lombok a history-service y orders-service pom.xml
3. [ ] Compilar proyecto sin errores: mvn clean compile
4. [ ] Ejecutar tests: mvn test
```

### FASE 2: VALIDACIÓN (4-6 horas)

```
5. [ ] Implementar validaciones en OrdersService:
        - Validar saldo disponible antes de compra
        - Validar posiciones antes de venta
        - Manejo de excepciones robusto

6. [ ] Integrar API externa (Market Data):
        - Yahoo Finance o Finnhub
        - Caché de cotizaciones
        - Manejo de errores de API

7. [ ] Crear DTOs propios en orders-service
8. [ ] Agregar logging (@Slf4j) en todos servicios
```

### FASE 3: DOCUMENTACIÓN Y TESTING (3-4 horas)

```
9. [ ] Documentación OpenAPI con Springdoc
10. [ ] Actualizar colección Postman
11. [ ] Crear tests unitarios básicos
12. [ ] Verificar funcionamiento end-to-end
```

### FASE 4: MEJORAS OPCIONALES (Suma nota) (4-8 horas)

```
13. [ ] Implementar logging centralizado
14. [ ] Circuit Breaker (Resilience4j)
15. [ ] Caché distribuida
16. [ ] Métricas y monitoring
17. [ ] Health checks custom
```

---

## 📝 CHECKLIST ANTES DE EXPOSICIÓN

```
ANTES DE EXPONER:
[ ] Resolver merge conflict
[ ] Proyecto compila sin errores: mvn clean install
[ ] BD levanta correctamente: make up (desde tpi-platform)
[ ] Todos los servicios arrancan sin errores
[ ] Postman collection testeada y funciona
[ ] Cotizaciones se obtienen de API (no hardcodeadas)
[ ] Órdenes se validan correctamente
[ ] Historial se registra completo
[ ] Logging visible en consola
[ ] Seguridad (OAuth2) funciona

EXTRAS PARA SUMA NOTA (15%):
[ ] Logging structured implementado
[ ] Circuit Breaker en llamadas inter-servicios
[ ] Caché de cotizaciones
[ ] Métricas expuestas (/metrics)
[ ] Health checks personalizados
[ ] Global exception handlers
[ ] OpenAPI/Swagger documentado
```

---

## 🚀 RESUMEN EJECUTIVO

| Aspecto | Calificación | Notas |
|---|---|---|
| **Cumplimiento Funcional** | 7/10 | Implementado pero con validaciones faltantes |
| **Arquitectura** | 8/10 | Buena separación, pero necesita mejorar observabilidad |
| **Seguridad** | 8/10 | OAuth2 correcto, pero falta hardening |
| **Código** | 5/10 | Merge conflict, Lombok mal usado, sin logging |
| **Testing** | 3/10 | Mínimo, necesita tests unitarios e integración |
| **Documentación** | 4/10 | Falta OpenAPI, Postman incompleta |
| **Extras** | 0/10 | Ninguno implementado (vale 15% de nota) |
| **PROMEDIO** | **5.6/10** | **APROBARÍA pero con áreas críticas** |

---

## 📞 RECOMENDACIÓN FINAL

**Si la exposición es en 2-3 días:**
- Enfocarse en Fase 1 y Fase 2 (lo crítico)
- Dejar Fase 3 como "en progreso"
- Extras son "nice to have" si sobra tiempo

**Si la exposición es en 1 semana:**
- Completar todas las fases
- Agregar mejoras (Fase 4)
- Tendrán ~80% calificación

**Si la exposición es en 2+ semanas:**
- Implementar todo + mejoras
- Agregar tests exhaustivos
- Documentación completa
- Tendrán ~95%+ calificación

