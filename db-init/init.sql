CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    balance_ars NUMERIC(19,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);

CREATE TABLE IF NOT EXISTS positions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    symbol VARCHAR(10) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    avg_price_ars NUMERIC(19,4) NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_positions_account_id ON positions(account_id);
CREATE INDEX IF NOT EXISTS idx_positions_symbol ON positions(symbol);

CREATE TABLE IF NOT EXISTS cash_movements (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    amount_ars NUMERIC(19,2) NOT NULL,
    reference_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cash_movements_account_id ON cash_movements(account_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_type ON cash_movements(type);

CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    symbol VARCHAR(10) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    remaining_quantity NUMERIC(19,2) NOT NULL,
    limit_price NUMERIC(19,4) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_symbol ON orders(symbol);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

CREATE TABLE IF NOT EXISTS order_fills (
    id UUID PRIMARY KEY,
    buy_order_id UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    sell_order_id UUID NOT NULL REFERENCES orders(id) ON DELETE RESTRICT,
    symbol VARCHAR(10) NOT NULL,
    quantity NUMERIC(19,2) NOT NULL,
    price_ars NUMERIC(19,4) NOT NULL,
    executed_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_order_fills_buy_order_id ON order_fills(buy_order_id);
CREATE INDEX IF NOT EXISTS idx_order_fills_sell_order_id ON order_fills(sell_order_id);
CREATE INDEX IF NOT EXISTS idx_order_fills_symbol ON order_fills(symbol);

CREATE TABLE IF NOT EXISTS history_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    user_id VARCHAR(255),
    order_id UUID,
    correlation_id UUID,
    causation_id UUID,
    payload_json JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_history_events_user_id ON history_events(user_id);
CREATE INDEX IF NOT EXISTS idx_history_events_event_type ON history_events(event_type);
CREATE INDEX IF NOT EXISTS idx_history_events_order_id ON history_events(order_id);
CREATE INDEX IF NOT EXISTS idx_history_events_occurred_at ON history_events(occurred_at);

CREATE TABLE IF NOT EXISTS user_operation_view (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    amount_ars NUMERIC(19,2),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_operation_view_user_id ON user_operation_view(user_id);

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    user_id VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details JSONB
);

CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_table_name ON audit_log(table_name);

INSERT INTO accounts (id, user_id, balance_ars, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440001'::uuid, 'juan@example.com', 50000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440002'::uuid, 'maria@example.com', 75000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440003'::uuid, 'carlos@example.com', 100000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440004'::uuid, 'ana@example.com', 125000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440005'::uuid, 'diego@example.com', 200000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440006'::uuid, 'lucia@example.com', 85000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440007'::uuid, 'pedro@example.com', 150000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440008'::uuid, 'sofia@example.com', 300000.00, NOW(), NOW());

INSERT INTO positions (id, account_id, symbol, quantity, avg_price_ars, updated_at) VALUES
('660e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'AAPL', 100.00, 150.50, NOW()),
('660e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'GOOGL', 50.00, 140.25, NOW()),
('660e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'MSFT', 75.00, 380.00, NOW()),
('660e8400-e29b-41d4-a716-446655440004'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'TSLA', 25.00, 240.50, NOW()),
('660e8400-e29b-41d4-a716-446655440005'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'AMZN', 30.00, 180.75, NOW()),
('660e8400-e29b-41d4-a716-446655440006'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'META', 60.00, 320.00, NOW()),
('660e8400-e29b-41d4-a716-446655440007'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'NVDA', 20.00, 875.50, NOW()),
('660e8400-e29b-41d4-a716-446655440008'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'AMD', 80.00, 155.25, NOW());

INSERT INTO cash_movements (id, account_id, type, amount_ars, reference_id, created_at) VALUES
('770e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'DEPOSIT', 50000.00, 'DEP-001-JUAN', NOW() - INTERVAL '30 days'),
('770e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'DEPOSIT', 75000.00, 'DEP-002-MARIA', NOW() - INTERVAL '25 days'),
('770e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'TRADE_SELL', 5400.00, 'TRD-001-CARLOS-AMZN', NOW() - INTERVAL '10 days'),
('770e8400-e29b-41d4-a716-446655440004'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'TRADE_BUY', -17510.00, 'TRD-001-ANA-NVDA', NOW() - INTERVAL '8 days'),
('770e8400-e29b-41d4-a716-446655440005'::uuid, '550e8400-e29b-41d4-a716-446655440005'::uuid, 'WITHDRAWAL', -10000.00, 'WTH-001-DIEGO', NOW() - INTERVAL '5 days'),
('770e8400-e29b-41d4-a716-446655440006'::uuid, '550e8400-e29b-41d4-a716-446655440006'::uuid, 'DEPOSIT', 30000.00, 'DEP-003-LUCIA', NOW() - INTERVAL '3 days'),
('770e8400-e29b-41d4-a716-446655440007'::uuid, '550e8400-e29b-41d4-a716-446655440007'::uuid, 'TRADE_BUY', -28575.00, 'TRD-002-PEDRO-AAPL', NOW() - INTERVAL '2 days'),
('770e8400-e29b-41d4-a716-446655440008'::uuid, '550e8400-e29b-41d4-a716-446655440008'::uuid, 'TRADE_SELL', 7650.00, 'TRD-002-SOFIA-GOOGL', NOW() - INTERVAL '1 day');

INSERT INTO orders (id, user_id, symbol, side, quantity, remaining_quantity, limit_price, status, created_at, updated_at) VALUES
('880e8400-e29b-41d4-a716-446655440001'::uuid, 'juan@example.com', 'AAPL', 'BUY', 100.00, 0.00, 155.00, 'FILLED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
('880e8400-e29b-41d4-a716-446655440002'::uuid, 'maria@example.com', 'MSFT', 'BUY', 75.00, 0.00, 385.00, 'FILLED', NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
('880e8400-e29b-41d4-a716-446655440003'::uuid, 'carlos@example.com', 'AMZN', 'BUY', 30.00, 10.00, 185.00, 'PARTIAL', NOW() - INTERVAL '15 days', NOW() - INTERVAL '10 days'),
('880e8400-e29b-41d4-a716-446655440004'::uuid, 'ana@example.com', 'NVDA', 'BUY', 20.00, 0.00, 880.00, 'FILLED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
('880e8400-e29b-41d4-a716-446655440005'::uuid, 'diego@example.com', 'TSLA', 'SELL', 50.00, 0.00, 260.00, 'FILLED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
('880e8400-e29b-41d4-a716-446655440006'::uuid, 'lucia@example.com', 'GOOGL', 'SELL', 40.00, 15.00, 145.00, 'PARTIAL', NOW() - INTERVAL '8 days', NOW() - INTERVAL '5 days'),
('880e8400-e29b-41d4-a716-446655440007'::uuid, 'pedro@example.com', 'META', 'SELL', 25.00, 25.00, 330.00, 'PENDING', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
('880e8400-e29b-41d4-a716-446655440008'::uuid, 'sofia@example.com', 'AMD', 'BUY', 60.00, 0.00, 160.00, 'FILLED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

INSERT INTO order_fills (id, buy_order_id, sell_order_id, symbol, quantity, price_ars, executed_at) VALUES
('990e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'AAPL', 50.00, 153.25, NOW() - INTERVAL '20 days'),
('990e8400-e29b-41d4-a716-446655440002'::uuid, '880e8400-e29b-41d4-a716-446655440002'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'MSFT', 40.00, 382.50, NOW() - INTERVAL '18 days'),
('990e8400-e29b-41d4-a716-446655440003'::uuid, '880e8400-e29b-41d4-a716-446655440003'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'AMZN', 20.00, 182.00, NOW() - INTERVAL '15 days'),
('990e8400-e29b-41d4-a716-446655440004'::uuid, '880e8400-e29b-41d4-a716-446655440004'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'NVDA', 15.00, 875.00, NOW() - INTERVAL '12 days'),
('990e8400-e29b-41d4-a716-446655440005'::uuid, '880e8400-e29b-41d4-a716-446655440002'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'TSLA', 30.00, 258.75, NOW() - INTERVAL '10 days'),
('990e8400-e29b-41d4-a716-446655440006'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'GOOGL', 25.00, 142.75, NOW() - INTERVAL '8 days'),
('990e8400-e29b-41d4-a716-446655440007'::uuid, '880e8400-e29b-41d4-a716-446655440008'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'AMD', 60.00, 157.50, NOW() - INTERVAL '1 day'),
('990e8400-e29b-41d4-a716-446655440008'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'AAPL', 50.00, 154.75, NOW() - INTERVAL '19 days');

INSERT INTO history_events (event_id, event_type, user_id, order_id, correlation_id, causation_id, payload_json, occurred_at) VALUES
('aa0e8400-e29b-41d4-a716-446655440001'::uuid, 'ACCOUNT_CREATED', 'juan@example.com', NULL, '550e8400-e29b-41d4-a716-446655440001'::uuid, NULL, '{"userId": "juan@example.com", "initialBalance": 50000.00, "currency": "ARS"}', NOW() - INTERVAL '30 days'),
('aa0e8400-e29b-41d4-a716-446655440002'::uuid, 'DEPOSIT_RECEIVED', 'maria@example.com', NULL, '550e8400-e29b-41d4-a716-446655440002'::uuid, NULL, '{"userId": "maria@example.com", "amount": 75000.00, "method": "bank_transfer"}', NOW() - INTERVAL '25 days'),
('aa0e8400-e29b-41d4-a716-446655440003'::uuid, 'ORDER_CREATED', 'juan@example.com', '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, NULL, '{"orderId": "880e8400-e29b-41d4-a716-446655440001", "symbol": "AAPL", "side": "BUY", "quantity": 100, "limitPrice": 155.00}', NOW() - INTERVAL '20 days'),
('aa0e8400-e29b-41d4-a716-446655440004'::uuid, 'ORDER_FILLED', 'juan@example.com', '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '990e8400-e29b-41d4-a716-446655440001'::uuid, '{"orderId": "880e8400-e29b-41d4-a716-446655440001", "totalFilled": 100, "avgPrice": 153.50}', NOW() - INTERVAL '20 days'),
('aa0e8400-e29b-41d4-a716-446655440005'::uuid, 'POSITION_OPENED', 'juan@example.com', NULL, '660e8400-e29b-41d4-a716-446655440001'::uuid, NULL, '{"symbol": "AAPL", "quantity": 100, "avgPrice": 153.50}', NOW() - INTERVAL '20 days'),
('aa0e8400-e29b-41d4-a716-446655440006'::uuid, 'ORDER_PARTIAL', 'carlos@example.com', '880e8400-e29b-41d4-a716-446655440003'::uuid, '880e8400-e29b-41d4-a716-446655440003'::uuid, NULL, '{"orderId": "880e8400-e29b-41d4-a716-446655440003", "filled": 20, "remaining": 10}', NOW() - INTERVAL '10 days'),
('aa0e8400-e29b-41d4-a716-446655440007'::uuid, 'TRADE_EXECUTED', 'diego@example.com', '880e8400-e29b-41d4-a716-446655440005'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, '990e8400-e29b-41d4-a716-446655440005'::uuid, '{"orderId": "880e8400-e29b-41d4-a716-446655440005", "symbol": "TSLA", "quantity": 30, "executedPrice": 258.75}', NOW() - INTERVAL '10 days'),
('aa0e8400-e29b-41d4-a716-446655440008'::uuid, 'PORTFOLIO_UPDATED', 'sofia@example.com', NULL, '550e8400-e29b-41d4-a716-446655440008'::uuid, NULL, '{"totalValue": 315250.00, "cashBalance": 300000.00, "portfolioValue": 15250.00}', NOW() - INTERVAL '1 day');

INSERT INTO user_operation_view (id, user_id, operation_type, symbol, amount_ars, created_at) VALUES
('bb0e8400-e29b-41d4-a716-446655440001'::uuid, 'juan@example.com', 'DEPOSIT', NULL, 50000.00, NOW() - INTERVAL '30 days'),
('bb0e8400-e29b-41d4-a716-446655440002'::uuid, 'juan@example.com', 'BUY_ORDER', 'AAPL', -15500.00, NOW() - INTERVAL '20 days'),
('bb0e8400-e29b-41d4-a716-446655440003'::uuid, 'maria@example.com', 'DEPOSIT', NULL, 75000.00, NOW() - INTERVAL '25 days'),
('bb0e8400-e29b-41d4-a716-446655440004'::uuid, 'carlos@example.com', 'BUY_ORDER', 'AMZN', -5550.00, NOW() - INTERVAL '15 days'),
('bb0e8400-e29b-41d4-a716-446655440005'::uuid, 'diego@example.com', 'SELL_ORDER', 'TSLA', 7740.00, NOW() - INTERVAL '10 days'),
('bb0e8400-e29b-41d4-a716-446655440006'::uuid, 'lucia@example.com', 'WITHDRAWAL', NULL, -10000.00, NOW() - INTERVAL '5 days'),
('bb0e8400-e29b-41d4-a716-446655440007'::uuid, 'pedro@example.com', 'SELL_ORDER', 'META', 0.00, NOW() - INTERVAL '3 days'),
('bb0e8400-e29b-41d4-a716-446655440008'::uuid, 'sofia@example.com', 'BUY_ORDER', 'AMD', -9450.00, NOW() - INTERVAL '1 day');

INSERT INTO audit_log (id, table_name, operation, user_id, timestamp, details) VALUES
('cc0e8400-e29b-41d4-a716-446655440001'::uuid, 'accounts', 'INSERT', 'system', NOW() - INTERVAL '30 days', '{"record_id": "550e8400-e29b-41d4-a716-446655440001", "user_id": "juan@example.com", "action": "Account creation"}'),
('cc0e8400-e29b-41d4-a716-446655440002'::uuid, 'cash_movements', 'INSERT', 'juan@example.com', NOW() - INTERVAL '30 days', '{"record_id": "770e8400-e29b-41d4-a716-446655440001", "type": "DEPOSIT", "amount": 50000.00}'),
('cc0e8400-e29b-41d4-a716-446655440003'::uuid, 'orders', 'INSERT', 'juan@example.com', NOW() - INTERVAL '20 days', '{"record_id": "880e8400-e29b-41d4-a716-446655440001", "symbol": "AAPL", "side": "BUY", "quantity": 100}'),
('cc0e8400-e29b-41d4-a716-446655440004'::uuid, 'orders', 'UPDATE', 'system', NOW() - INTERVAL '20 days', '{"record_id": "880e8400-e29b-41d4-a716-446655440001", "status": "PENDING -> FILLED"}'),
('cc0e8400-e29b-41d4-a716-446655440005'::uuid, 'positions', 'INSERT', 'system', NOW() - INTERVAL '20 days', '{"record_id": "660e8400-e29b-41d4-a716-446655440001", "symbol": "AAPL", "quantity": 100}'),
('cc0e8400-e29b-41d4-a716-446655440006'::uuid, 'order_fills', 'INSERT', 'system', NOW() - INTERVAL '10 days', '{"record_id": "990e8400-e29b-41d4-a716-446655440005", "symbol": "TSLA", "quantity": 30, "price": 258.75}'),
('cc0e8400-e29b-41d4-a716-446655440007'::uuid, 'cash_movements', 'INSERT', 'diego@example.com', NOW() - INTERVAL '10 days', '{"record_id": "770e8400-e29b-41d4-a716-446655440005", "type": "TRADE_SELL", "amount": 7740.00}'),
('cc0e8400-e29b-41d4-a716-446655440008'::uuid, 'orders', 'INSERT', 'sofia@example.com', NOW() - INTERVAL '1 day', '{"record_id": "880e8400-e29b-41d4-a716-446655440008", "symbol": "AMD", "side": "BUY", "quantity": 60}');
