SET search_path TO orders, public;

CREATE TABLE IF NOT EXISTS orders (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id varchar(100) NOT NULL,
  symbol varchar(16) NOT NULL,
  side varchar(8) NOT NULL CHECK (side IN ('BUY', 'SELL')),
  quantity numeric(18,4) NOT NULL,
  remaining_quantity numeric(18,4) NOT NULL,
  limit_price numeric(18,4) NOT NULL,
  status varchar(20) NOT NULL CHECK (status IN ('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
  created_at timestamp NOT NULL DEFAULT now(),
  updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_fills (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  buy_order_id uuid NOT NULL REFERENCES orders(id),
  sell_order_id uuid NOT NULL REFERENCES orders(id),
  symbol varchar(16) NOT NULL,
  quantity numeric(18,4) NOT NULL,
  price_ars numeric(18,4) NOT NULL,
  executed_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_orders_symbol_status ON orders(symbol, status);
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);

