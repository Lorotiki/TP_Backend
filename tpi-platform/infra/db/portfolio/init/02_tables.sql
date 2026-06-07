SET search_path TO portfolio, public;

CREATE TABLE IF NOT EXISTS accounts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id varchar(100) NOT NULL UNIQUE,
  balance_ars numeric(18,2) NOT NULL DEFAULT 0,
  created_at timestamp NOT NULL DEFAULT now(),
  updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS positions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id uuid NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  symbol varchar(16) NOT NULL,
  quantity numeric(18,4) NOT NULL,
  avg_price_ars numeric(18,4) NOT NULL,
  updated_at timestamp NOT NULL DEFAULT now(),
  UNIQUE (account_id, symbol)
);

CREATE TABLE IF NOT EXISTS cash_movements (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id uuid NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
  type varchar(20) NOT NULL CHECK (type IN ('DEPOSIT', 'BUY_DEBIT', 'SELL_CREDIT')),
  amount_ars numeric(18,2) NOT NULL,
  reference_id varchar(100),
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_positions_symbol ON positions(symbol);
CREATE INDEX IF NOT EXISTS idx_cash_movements_account_id ON cash_movements(account_id);

