SET search_path TO portfolio, public;

INSERT INTO accounts (id, user_id, balance_ars)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'user-demo-1', 5000000.00),
  ('00000000-0000-0000-0000-000000000002', 'user-demo-2', 2500000.00)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO positions (account_id, symbol, quantity, avg_price_ars)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'NVDA', 50.0000, 35000.0000),
  ('00000000-0000-0000-0000-000000000002', 'AAPL', 30.0000, 27000.0000)
ON CONFLICT (account_id, symbol) DO NOTHING;

INSERT INTO cash_movements (account_id, type, amount_ars, reference_id)
VALUES
  ('00000000-0000-0000-0000-000000000001', 'DEPOSIT', 5000000.00, 'seed-deposit-1'),
  ('00000000-0000-0000-0000-000000000002', 'DEPOSIT', 2500000.00, 'seed-deposit-2');

