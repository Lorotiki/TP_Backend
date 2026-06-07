SET search_path TO history, public;

CREATE TABLE IF NOT EXISTS history_events (
  event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type varchar(64) NOT NULL,
  user_id varchar(100),
  order_id uuid,
  correlation_id uuid,
  causation_id uuid,
  payload_json jsonb NOT NULL,
  occurred_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_operation_view (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id varchar(100) NOT NULL,
  operation_type varchar(64) NOT NULL,
  symbol varchar(16),
  amount_ars numeric(18,2),
  created_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_history_events_user_id ON history_events(user_id);
CREATE INDEX IF NOT EXISTS idx_history_events_occurred_at ON history_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_user_operation_view_user_id ON user_operation_view(user_id);

