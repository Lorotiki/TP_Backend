-- ============================================================================
-- UNIFIED DATABASE SCHEMA FOR TPI BACKEND 2026
-- ============================================================================
-- Single consolidated database: stockmarket
-- All tables without separate schemas (using public schema)
-- ============================================================================

-- PORTFOLIO MODULE TABLES
-- ============================================================================

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

-- ORDERS MODULE TABLES
-- ============================================================================

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

-- HISTORY MODULE TABLES
-- ============================================================================

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
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_operation_view_user_id ON user_operation_view(user_id);

-- AUDIT/METADATA TABLE (Optional but useful)
-- ============================================================================

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

-- COMMENTS/DOCUMENTATION
-- ============================================================================
COMMENT ON TABLE accounts IS 'User portfolio accounts with ARS balance';
COMMENT ON TABLE positions IS 'Stock positions owned by users';
COMMENT ON TABLE cash_movements IS 'All cash deposits, withdrawals, and adjustments';
COMMENT ON TABLE orders IS 'Buy/Sell orders placed by users';
COMMENT ON TABLE order_fills IS 'Matches between buy and sell orders';
COMMENT ON TABLE history_events IS 'Event sourcing log for all system events';
COMMENT ON TABLE user_operation_view IS 'View of user operations for history tracking';
COMMENT ON TABLE audit_log IS 'Audit trail for data changes';
