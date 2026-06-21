# Datos de Prueba - TPI Backend 2026

## 📊 Resumen

Se crearon **8 filas por tabla** con datos coherentes y realistas siguiendo las reglas de negocio de la plataforma de trading.

## 👥 Usuarios de Prueba (8 Accounts)

| ID | Email | Balance ARS | Estado |
|---|---|---|---|
| 1 | juan@example.com | $50,000.00 | Activo |
| 2 | maria@example.com | $75,000.00 | Activo |
| 3 | carlos@example.com | $100,000.00 | Activo |
| 4 | ana@example.com | $125,000.00 | Activo |
| 5 | diego@example.com | $200,000.00 | Activo |
| 6 | lucia@example.com | $85,000.00 | Activo |
| 7 | pedro@example.com | $150,000.00 | Activo |
| 8 | sofia@example.com | $300,000.00 | Activo |

## 📈 Posiciones (Positions) - 8 Holdings

Cada usuario tiene 1-2 posiciones de stocks:

```
Juan:        100 AAPL @ 150.50 ARS
             50 GOOGL @ 140.25 ARS

Maria:       75 MSFT @ 380.00 ARS
             25 TSLA @ 240.50 ARS

Carlos:      30 AMZN @ 180.75 ARS
             60 META @ 320.00 ARS

Ana:         20 NVDA @ 875.50 ARS
             80 AMD @ 155.25 ARS
```

**Inversiones Totales**: ~$250,000 en acciones

## 💰 Movimientos de Efectivo (Cash Movements) - 8 Transacciones

| Tipo | Usuario | Monto | Referencia |
|------|---------|-------|-----------|
| DEPOSIT | Juan | +$50,000 | DEP-001-JUAN |
| DEPOSIT | Maria | +$75,000 | DEP-002-MARIA |
| TRADE_SELL | Carlos | +$5,400 | AMZN sale |
| TRADE_BUY | Ana | -$17,510 | NVDA purchase |
| WITHDRAWAL | Diego | -$10,000 | WTH-001-DIEGO |
| DEPOSIT | Lucia | +$30,000 | DEP-003-LUCIA |
| TRADE_BUY | Pedro | -$28,575 | AAPL purchase |
| TRADE_SELL | Sofia | +$7,650 | GOOGL sale |

## 📋 Órdenes (Orders) - 8 Órdenes

| ID | Usuario | Símbolo | Tipo | Cantidad | Estado | Precio Límite |
|---|---|---|---|---|---|---|
| 1 | Juan | AAPL | BUY | 100 | **FILLED** ✅ | 155.00 |
| 2 | Maria | MSFT | BUY | 75 | **FILLED** ✅ | 385.00 |
| 3 | Carlos | AMZN | BUY | 30 | **PARTIAL** ⏳ (20/30) | 185.00 |
| 4 | Ana | NVDA | BUY | 20 | **FILLED** ✅ | 880.00 |
| 5 | Diego | TSLA | SELL | 50 | **FILLED** ✅ | 260.00 |
| 6 | Lucia | GOOGL | SELL | 40 | **PARTIAL** ⏳ (25/40) | 145.00 |
| 7 | Pedro | META | SELL | 25 | **PENDING** ⏹️ | 330.00 |
| 8 | Sofia | AMD | BUY | 60 | **FILLED** ✅ | 160.00 |

**Mix de Estados**:
- ✅ **FILLED** (5): Órdenes completamente ejecutadas
- ⏳ **PARTIAL** (2): Órdenes parcialmente ejecutadas
- ⏹️ **PENDING** (1): Órdenes en espera

## 🔄 Ejecuciones de Órdenes (Order Fills) - 8 Matches

Cada "order fill" representa un match entre una orden de compra y una de venta:

| Match | Comprador | Vendedor | Símbolo | Cantidad | Precio Ejecutado |
|-------|-----------|----------|---------|----------|------------------|
| 1 | Juan BUY | Other SELL | AAPL | 50 | 153.25 |
| 2 | Maria BUY | Other SELL | MSFT | 40 | 382.50 |
| 3 | Carlos BUY | Other SELL | AMZN | 20 | 182.00 |
| 4 | Ana BUY | Other SELL | NVDA | 15 | 875.00 |
| 5 | Other BUY | Diego SELL | TSLA | 30 | 258.75 |
| 6 | Other BUY | Lucia SELL | GOOGL | 25 | 142.75 |
| 7 | Sofia BUY | Other SELL | AMD | 60 | 157.50 |
| 8 | Juan BUY | Other SELL | AAPL | 50 | 154.75 |

## 📝 Eventos de Historial (History Events) - 8 Eventos

Usando **Event Sourcing** para auditoría completa:

1. **ACCOUNT_CREATED**: Creación de cuenta Juan
2. **DEPOSIT_RECEIVED**: Depósito de Maria
3. **ORDER_CREATED**: Orden BUY AAPL de Juan
4. **ORDER_FILLED**: Orden FILLED AAPL de Juan
5. **POSITION_OPENED**: Posición abierta AAPL
6. **ORDER_PARTIAL**: Orden parcial AMZN de Carlos
7. **TRADE_EXECUTED**: Trade ejecutado TSLA de Diego
8. **PORTFOLIO_UPDATED**: Portfolio actualizado Sofia

Cada evento contiene:
- `event_type`: Tipo de evento
- `user_id`: Usuario involucrado
- `payload_json`: Datos del evento (JSON)
- `correlation_id`: Para rastrear flujos
- `causation_id`: Qué evento causó este

## 📊 Vista de Operaciones (User Operation View) - 8 Operaciones

Resumen legible de operaciones por usuario:

```
Juan:       Deposit $50,000 (30 días atrás)
            Buy 100 AAPL @ 155 (20 días atrás)

Maria:      Deposit $75,000 (25 días atrás)
            Buy 75 MSFT @ 385 (18 días atrás)

Carlos:     Buy 30 AMZN @ 185 (PARTIAL - 20/30 filled)

Diego:      Sell 50 TSLA @ 260 (FULLY executed)

Lucia:      Withdraw $10,000 (5 días atrás)

Pedro:      Sell 25 META @ 330 (PENDING)

Sofia:      Buy 60 AMD @ 160 (FULLY executed)
```

## 🔐 Log de Auditoría (Audit Log) - 8 Registros

Rastreo de todas las operaciones:

```
INSERT accounts (juan@example.com)
INSERT cash_movements (DEPOSIT $50,000)
INSERT orders (AAPL BUY)
UPDATE orders (status: PENDING → FILLED)
INSERT positions (AAPL 100 shares)
INSERT order_fills (TSLA $30 executed)
INSERT cash_movements (TRADE_SELL +$7,740)
INSERT orders (AMD BUY)
```

## 🧪 Cómo Verificar los Datos

### 1. Ver Todas las Cuentas

```sql
SELECT user_id, balance_ars, created_at 
FROM accounts 
ORDER BY created_at DESC;
```

**Resultado esperado**: 8 usuarios con diferentes balances

### 2. Ver Portafolios (Cuentas + Posiciones)

```sql
SELECT 
    a.user_id,
    a.balance_ars as cash,
    COUNT(p.id) as num_positions,
    SUM(p.quantity * p.avg_price_ars) as portfolio_value
FROM accounts a
LEFT JOIN positions p ON a.id = p.account_id
GROUP BY a.id, a.user_id, a.balance_ars
ORDER BY a.balance_ars DESC;
```

**Resultado esperado**: Portfolio summary de 8 usuarios

### 3. Ver Órdenes por Estado

```sql
SELECT status, COUNT(*) as cantidad, SUM(quantity) as total_shares
FROM orders
GROUP BY status
ORDER BY status;
```

**Resultado esperado**:
```
FILLED      | 5 | 345.00
PARTIAL     | 2 | 65.00
PENDING     | 1 | 25.00
```

### 4. Ver Historial de Trading de un Usuario

```sql
SELECT 
    o.user_id,
    o.symbol,
    o.side,
    o.quantity,
    o.remaining_quantity,
    o.status,
    o.created_at
FROM orders o
WHERE o.user_id = 'juan@example.com'
ORDER BY o.created_at DESC;
```

### 5. Ver Ejecuciones de Órdenes

```sql
SELECT 
    of.symbol,
    of.quantity,
    of.price_ars,
    of.executed_at,
    (SELECT symbol FROM orders WHERE id = of.buy_order_id) as buyer,
    (SELECT symbol FROM orders WHERE id = of.sell_order_id) as seller
FROM order_fills of
ORDER BY of.executed_at DESC;
```

### 6. Ver Eventos de un Usuario

```sql
SELECT 
    event_id,
    event_type,
    occurred_at,
    payload_json
FROM history_events
WHERE user_id = 'juan@example.com'
ORDER BY occurred_at DESC;
```

### 7. Ver Log de Auditoría

```sql
SELECT 
    table_name,
    operation,
    user_id,
    timestamp,
    details
FROM audit_log
ORDER BY timestamp DESC;
```

## 🚀 Levantar con Datos de Prueba

```bash
# Reset completo (elimina volumen previo)
docker compose down -v

# Levantar todo
docker compose up -d --build

# Esperar a que PostgreSQL inicie
sleep 15

# Verificar que los datos se insertaron
docker exec -it postgres psql -U admin -d stockmarket -c "SELECT COUNT(*) FROM accounts;"

# Debería retornar: 8
```

## 📈 Datos Coherentes

Los datos de prueba fueron diseñados para ser **realistas y coherentes**:

✅ **Temporalidad**: Las transacciones tienen fechas progresivas (más antiguas primero)
✅ **Relaciones**: Posiciones corresponden a órdenes ejecutadas
✅ **Estados Variados**: Mix de FILLED, PARTIAL, PENDING
✅ **Valores Realistas**: Precios similares a acciones reales
✅ **Auditoría Completa**: Todos los eventos registrados
✅ **Event Sourcing**: Cada cambio tiene su evento correspondiente

## 🔄 Flujo de Negocio Representado

1. **Usuario crea cuenta** → ACCOUNT_CREATED event
2. **Usuario deposita dinero** → DEPOSIT + CASH_MOVEMENT
3. **Usuario crea orden** → ORDER_CREATED + HISTORY_EVENT
4. **Orden se ejecuta** → ORDER_FILLED + ORDER_FILL + POSITION_OPENED
5. **Se registra en auditoría** → AUDIT_LOG entry
6. **Se refleja en vista de operaciones** → USER_OPERATION_VIEW

## 📊 Estadísticas de los Datos

- **Total de cuentas**: 8
- **Total de posiciones**: 8 (1-2 por usuario)
- **Total de cash movements**: 8 (depósitos, retiros, trades)
- **Total de órdenes**: 8 (5 filled, 2 partial, 1 pending)
- **Total de order fills**: 8 (matches ejecutados)
- **Total de eventos**: 8 (auditoría completa)
- **Total de operaciones**: 8 (vistas de usuario)
- **Total de auditoría**: 8 (trail de cambios)

**Total de registros**: 64 filas distribuidas en 8 tablas

---

**Última actualización**: 2026-06-20
**Base de datos**: stockmarket
