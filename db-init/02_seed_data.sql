-- ============================================================================
-- SEED DATA FOR TPI BACKEND 2026 - TEST DATA
-- ============================================================================
-- 8 rows per table with realistic business logic
-- ============================================================================

-- PORTFOLIO ACCOUNTS (8 users)
-- ============================================================================
INSERT INTO accounts (id, user_id, balance_ars, created_at, updated_at) VALUES
('550e8400-e29b-41d4-a716-446655440001'::uuid, 'juan@example.com', 50000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440002'::uuid, 'maria@example.com', 75000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440003'::uuid, 'carlos@example.com', 100000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440004'::uuid, 'ana@example.com', 125000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440005'::uuid, 'diego@example.com', 200000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440006'::uuid, 'lucia@example.com', 85000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440007'::uuid, 'pedro@example.com', 150000.00, NOW(), NOW()),
('550e8400-e29b-41d4-a716-446655440008'::uuid, 'sofia@example.com', 300000.00, NOW(), NOW());

-- POSITIONS (Stock holdings per user - 8 positions)
-- ============================================================================
INSERT INTO positions (id, account_id, symbol, quantity, avg_price_ars, updated_at) VALUES
-- Juan's AAPL and GOOGL positions
('660e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'AAPL', 100.00, 150.50, NOW()),
('660e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'GOOGL', 50.00, 140.25, NOW()),
-- Maria's MSFT and TSLA positions
('660e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'MSFT', 75.00, 380.00, NOW()),
('660e8400-e29b-41d4-a716-446655440004'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'TSLA', 25.00, 240.50, NOW()),
-- Carlos's AMZN and META positions
('660e8400-e29b-41d4-a716-446655440005'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'AMZN', 30.00, 180.75, NOW()),
('660e8400-e29b-41d4-a716-446655440006'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'META', 60.00, 320.00, NOW()),
-- Ana's NVDA and AMD positions
('660e8400-e29b-41d4-a716-446655440007'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'NVDA', 20.00, 875.50, NOW()),
('660e8400-e29b-41d4-a716-446655440008'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'AMD', 80.00, 155.25, NOW());

-- CASH MOVEMENTS (Deposits, withdrawals, trades - 8 movements)
-- ============================================================================
INSERT INTO cash_movements (id, account_id, type, amount_ars, reference_id, created_at) VALUES
-- Deposits
('770e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid, 'DEPOSIT', 50000.00, 'DEP-001-JUAN', NOW() - INTERVAL '30 days'),
('770e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid, 'DEPOSIT', 75000.00, 'DEP-002-MARIA', NOW() - INTERVAL '25 days'),
-- Trade executions (sell positions)
('770e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440003'::uuid, 'TRADE_SELL', 5400.00, 'TRD-001-CARLOS-AMZN', NOW() - INTERVAL '10 days'),
('770e8400-e29b-41d4-a716-446655440004'::uuid, '550e8400-e29b-41d4-a716-446655440004'::uuid, 'TRADE_BUY', -17510.00, 'TRD-001-ANA-NVDA', NOW() - INTERVAL '8 days'),
-- Withdrawals
('770e8400-e29b-41d4-a716-446655440005'::uuid, '550e8400-e29b-41d4-a716-446655440005'::uuid, 'WITHDRAWAL', -10000.00, 'WTH-001-DIEGO', NOW() - INTERVAL '5 days'),
('770e8400-e29b-41d4-a716-446655440006'::uuid, '550e8400-e29b-41d4-a716-446655440006'::uuid, 'DEPOSIT', 30000.00, 'DEP-003-LUCIA', NOW() - INTERVAL '3 days'),
-- More trade executions
('770e8400-e29b-41d4-a716-446655440007'::uuid, '550e8400-e29b-41d4-a716-446655440007'::uuid, 'TRADE_BUY', -28575.00, 'TRD-002-PEDRO-AAPL', NOW() - INTERVAL '2 days'),
('770e8400-e29b-41d4-a716-446655440008'::uuid, '550e8400-e29b-41d4-a716-446655440008'::uuid, 'TRADE_SELL', 7650.00, 'TRD-002-SOFIA-GOOGL', NOW() - INTERVAL '1 day');

-- ORDERS (Buy/Sell orders - 8 orders with mix of statuses)
-- ============================================================================
INSERT INTO orders (id, user_id, symbol, side, quantity, remaining_quantity, limit_price, status, created_at, updated_at) VALUES
-- BUY Orders
('880e8400-e29b-41d4-a716-446655440001'::uuid, 'juan@example.com', 'AAPL', 'BUY', 100.00, 0.00, 155.00, 'FILLED', NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'),
('880e8400-e29b-41d4-a716-446655440002'::uuid, 'maria@example.com', 'MSFT', 'BUY', 75.00, 0.00, 385.00, 'FILLED', NOW() - INTERVAL '18 days', NOW() - INTERVAL '18 days'),
('880e8400-e29b-41d4-a716-446655440003'::uuid, 'carlos@example.com', 'AMZN', 'BUY', 30.00, 10.00, 185.00, 'PARTIAL', NOW() - INTERVAL '15 days', NOW() - INTERVAL '10 days'),
('880e8400-e29b-41d4-a716-446655440004'::uuid, 'ana@example.com', 'NVDA', 'BUY', 20.00, 0.00, 880.00, 'FILLED', NOW() - INTERVAL '12 days', NOW() - INTERVAL '12 days'),
-- SELL Orders
('880e8400-e29b-41d4-a716-446655440005'::uuid, 'diego@example.com', 'TSLA', 'SELL', 50.00, 0.00, 260.00, 'FILLED', NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
('880e8400-e29b-41d4-a716-446655440006'::uuid, 'lucia@example.com', 'GOOGL', 'SELL', 40.00, 15.00, 145.00, 'PARTIAL', NOW() - INTERVAL '8 days', NOW() - INTERVAL '5 days'),
('880e8400-e29b-41d4-a716-446655440007'::uuid, 'pedro@example.com', 'META', 'SELL', 25.00, 25.00, 330.00, 'PENDING', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
('880e8400-e29b-41d4-a716-446655440008'::uuid, 'sofia@example.com', 'AMD', 'BUY', 60.00, 0.00, 160.00, 'FILLED', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day');

-- ORDER FILLS (Matches between buy and sell orders - 8 fills)
-- ============================================================================
INSERT INTO order_fills (id, buy_order_id, sell_order_id, symbol, quantity, price_ars, executed_at) VALUES
-- Fill 1: Juan's AAPL BUY matched with a seller
('990e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'AAPL', 50.00, 153.25, NOW() - INTERVAL '20 days'),
-- Fill 2: Maria's MSFT BUY matched
('990e8400-e29b-41d4-a716-446655440002'::uuid, '880e8400-e29b-41d4-a716-446655440002'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'MSFT', 40.00, 382.50, NOW() - INTERVAL '18 days'),
-- Fill 3: Carlos's AMZN partial BUY matched
('990e8400-e29b-41d4-a716-446655440003'::uuid, '880e8400-e29b-41d4-a716-446655440003'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'AMZN', 20.00, 182.00, NOW() - INTERVAL '15 days'),
-- Fill 4: Ana's NVDA BUY matched
('990e8400-e29b-41d4-a716-446655440004'::uuid, '880e8400-e29b-41d4-a716-446655440004'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'NVDA', 15.00, 875.00, NOW() - INTERVAL '12 days'),
-- Fill 5: Diego's TSLA SELL matched
('990e8400-e29b-41d4-a716-446655440005'::uuid, '880e8400-e29b-41d4-a716-446655440002'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'TSLA', 30.00, 258.75, NOW() - INTERVAL '10 days'),
-- Fill 6: Lucia's GOOGL partial SELL matched
('990e8400-e29b-41d4-a716-446655440006'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'GOOGL', 25.00, 142.75, NOW() - INTERVAL '8 days'),
-- Fill 7: Sofia's AMD BUY matched
('990e8400-e29b-41d4-a716-446655440007'::uuid, '880e8400-e29b-41d4-a716-446655440008'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, 'AMD', 60.00, 157.50, NOW() - INTERVAL '1 day'),
-- Fill 8: Juan's AAPL BUY remaining matched
('990e8400-e29b-41d4-a716-446655440008'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440006'::uuid, 'AAPL', 50.00, 154.75, NOW() - INTERVAL '19 days');

-- HISTORY EVENTS (Event sourcing log - 8 events)
-- ============================================================================
INSERT INTO history_events (event_id, event_type, user_id, order_id, correlation_id, causation_id, payload_json, occurred_at) VALUES
('aa0e8400-e29b-41d4-a716-446655440001'::uuid, 'ACCOUNT_CREATED', 'juan@example.com', NULL, '550e8400-e29b-41d4-a716-446655440001'::uuid, NULL, '{"userId": "juan@example.com", "initialBalance": 50000.00, "currency": "ARS"}', NOW() - INTERVAL '30 days'),
('aa0e8400-e29b-41d4-a716-446655440002'::uuid, 'DEPOSIT_RECEIVED', 'maria@example.com', NULL, '550e8400-e29b-41d4-a716-446655440002'::uuid, NULL, '{"userId": "maria@example.com", "amount": 75000.00, "method": "bank_transfer"}', NOW() - INTERVAL '25 days'),
('aa0e8400-e29b-41d4-a716-446655440003'::uuid, 'ORDER_CREATED', 'juan@example.com', '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, NULL, '{"orderId": "880e8400-e29b-41d4-a716-446655440001", "symbol": "AAPL", "side": "BUY", "quantity": 100, "limitPrice": 155.00}', NOW() - INTERVAL '20 days'),
('aa0e8400-e29b-41d4-a716-446655440004'::uuid, 'ORDER_FILLED', 'juan@example.com', '880e8400-e29b-41d4-a716-446655440001'::uuid, '880e8400-e29b-41d4-a716-446655440001'::uuid, '990e8400-e29b-41d4-a716-446655440001'::uuid, '{"orderId": "880e8400-e29b-41d4-a716-446655440001", "totalFilled": 100, "avgPrice": 153.50}', NOW() - INTERVAL '20 days'),
('aa0e8400-e29b-41d4-a716-446655440005'::uuid, 'POSITION_OPENED', 'juan@example.com', NULL, '660e8400-e29b-41d4-a716-446655440001'::uuid, NULL, '{"symbol": "AAPL", "quantity": 100, "avgPrice": 153.50}', NOW() - INTERVAL '20 days'),
('aa0e8400-e29b-41d4-a716-446655440006'::uuid, 'ORDER_PARTIAL', 'carlos@example.com', '880e8400-e29b-41d4-a716-446655440003'::uuid, '880e8400-e29b-41d4-a716-446655440003'::uuid, NULL, '{"orderId": "880e8400-e29b-41d4-a716-446655440003", "filled": 20, "remaining": 10}', NOW() - INTERVAL '10 days'),
('aa0e8400-e29b-41d4-a716-446655440007'::uuid, 'TRADE_EXECUTED', 'diego@example.com', '880e8400-e29b-41d4-a716-446655440005'::uuid, '880e8400-e29b-41d4-a716-446655440005'::uuid, '990e8400-e29b-41d4-a716-446655440005'::uuid, '{"orderId": "880e8400-e29b-41d4-a716-446655440005", "symbol": "TSLA", "quantity": 30, "executedPrice": 258.75}', NOW() - INTERVAL '10 days'),
('aa0e8400-e29b-41d4-a716-446655440008'::uuid, 'PORTFOLIO_UPDATED', 'sofia@example.com', NULL, '550e8400-e29b-41d4-a716-446655440008'::uuid, NULL, '{"totalValue": 315250.00, "cashBalance": 300000.00, "portfolioValue": 15250.00}', NOW() - INTERVAL '1 day');

-- USER OPERATION VIEW (Operation history view - 8 operations)
-- ============================================================================
INSERT INTO user_operation_view (id, user_id, operation_type, description, created_at) VALUES
('bb0e8400-e29b-41d4-a716-446655440001'::uuid, 'juan@example.com', 'DEPOSIT', 'Initial deposit of 50,000 ARS', NOW() - INTERVAL '30 days'),
('bb0e8400-e29b-41d4-a716-446655440002'::uuid, 'juan@example.com', 'BUY_ORDER', 'Bought 100 AAPL @ 155.00 ARS', NOW() - INTERVAL '20 days'),
('bb0e8400-e29b-41d4-a716-446655440003'::uuid, 'maria@example.com', 'DEPOSIT', 'Initial deposit of 75,000 ARS', NOW() - INTERVAL '25 days'),
('bb0e8400-e29b-41d4-a716-446655440004'::uuid, 'carlos@example.com', 'BUY_ORDER', 'Bought 30 AMZN @ 185.00 ARS (Partial: 20 filled, 10 pending)', NOW() - INTERVAL '15 days'),
('bb0e8400-e29b-41d4-a716-446655440005'::uuid, 'diego@example.com', 'SELL_ORDER', 'Sold 50 TSLA @ 260.00 ARS (Fully executed)', NOW() - INTERVAL '10 days'),
('bb0e8400-e29b-41d4-a716-446655440006'::uuid, 'lucia@example.com', 'WITHDRAWAL', 'Withdrawn 10,000 ARS to bank account', NOW() - INTERVAL '5 days'),
('bb0e8400-e29b-41d4-a716-446655440007'::uuid, 'pedro@example.com', 'SELL_ORDER', 'Pending order: Sell 25 META @ 330.00 ARS', NOW() - INTERVAL '3 days'),
('bb0e8400-e29b-41d4-a716-446655440008'::uuid, 'sofia@example.com', 'BUY_ORDER', 'Bought 60 AMD @ 160.00 ARS (Fully executed)', NOW() - INTERVAL '1 day');

-- AUDIT LOG (Audit trail - 8 entries)
-- ============================================================================
INSERT INTO audit_log (id, table_name, operation, user_id, timestamp, details) VALUES
('cc0e8400-e29b-41d4-a716-446655440001'::uuid, 'accounts', 'INSERT', 'system', NOW() - INTERVAL '30 days', '{"record_id": "550e8400-e29b-41d4-a716-446655440001", "user_id": "juan@example.com", "action": "Account creation"}'),
('cc0e8400-e29b-41d4-a716-446655440002'::uuid, 'cash_movements', 'INSERT', 'juan@example.com', NOW() - INTERVAL '30 days', '{"record_id": "770e8400-e29b-41d4-a716-446655440001", "type": "DEPOSIT", "amount": 50000.00}'),
('cc0e8400-e29b-41d4-a716-446655440003'::uuid, 'orders', 'INSERT', 'juan@example.com', NOW() - INTERVAL '20 days', '{"record_id": "880e8400-e29b-41d4-a716-446655440001", "symbol": "AAPL", "side": "BUY", "quantity": 100}'),
('cc0e8400-e29b-41d4-a716-446655440004'::uuid, 'orders', 'UPDATE', 'system', NOW() - INTERVAL '20 days', '{"record_id": "880e8400-e29b-41d4-a716-446655440001", "status": "PENDING -> FILLED"}'),
('cc0e8400-e29b-41d4-a716-446655440005'::uuid, 'positions', 'INSERT', 'system', NOW() - INTERVAL '20 days', '{"record_id": "660e8400-e29b-41d4-a716-446655440001", "symbol": "AAPL", "quantity": 100}'),
('cc0e8400-e29b-41d4-a716-446655440006'::uuid, 'order_fills', 'INSERT', 'system', NOW() - INTERVAL '10 days', '{"record_id": "990e8400-e29b-41d4-a716-446655440005", "symbol": "TSLA", "quantity": 30, "price": 258.75}'),
('cc0e8400-e29b-41d4-a716-446655440007'::uuid, 'cash_movements', 'INSERT', 'diego@example.com', NOW() - INTERVAL '10 days', '{"record_id": "770e8400-e29b-41d4-a716-446655440005", "type": "TRADE_SELL", "amount": 7740.00}'),
('cc0e8400-e29b-41d4-a716-446655440008'::uuid, 'orders', 'INSERT', 'sofia@example.com', NOW() - INTERVAL '1 day', '{"record_id": "880e8400-e29b-41d4-a716-446655440008", "symbol": "AMD", "side": "BUY", "quantity": 60}');

-- ============================================================================
-- DATA VALIDATION QUERIES (Optional - run these to verify the data)
-- ============================================================================

-- Verify account count
-- SELECT COUNT(*) as account_count FROM accounts;
-- Expected: 8

-- Verify total positions
-- SELECT COUNT(*) as position_count FROM positions;
-- Expected: 8

-- Verify cash movements
-- SELECT COUNT(*) as movement_count FROM cash_movements;
-- Expected: 8

-- Verify orders by status
-- SELECT status, COUNT(*) FROM orders GROUP BY status;
-- Expected: FILLED (5), PARTIAL (2), PENDING (1)

-- Verify order fills
-- SELECT COUNT(*) as fill_count FROM order_fills;
-- Expected: 8

-- View user portfolio summary
-- SELECT u.user_id, COUNT(p.id) as positions, a.balance_ars as cash_balance
-- FROM accounts a
-- LEFT JOIN positions p ON a.id = p.account_id
-- GROUP BY u.id, a.balance_ars;

-- ============================================================================
