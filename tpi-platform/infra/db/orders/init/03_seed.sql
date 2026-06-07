SET search_path TO orders, public;

INSERT INTO orders (id, user_id, symbol, side, quantity, remaining_quantity, limit_price, status)
VALUES
  ('10000000-0000-0000-0000-000000000001', 'user-demo-2', 'NVDA', 'SELL', 40.0000, 40.0000, 36000.0000, 'PENDING'),
  ('10000000-0000-0000-0000-000000000002', 'user-demo-1', 'AAPL', 'BUY', 10.0000, 10.0000, 28000.0000, 'PENDING')
ON CONFLICT (id) DO NOTHING;

