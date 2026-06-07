SET search_path TO history, public;

INSERT INTO history_events (event_id, event_type, user_id, order_id, correlation_id, causation_id, payload_json)
VALUES
  (
    '20000000-0000-0000-0000-000000000001',
    'DEPOSIT_CREATED',
    'user-demo-1',
    NULL,
    '30000000-0000-0000-0000-000000000001',
    NULL,
    '{"amountArs": 5000000.00, "source": "seed"}'::jsonb
  )
ON CONFLICT (event_id) DO NOTHING;

INSERT INTO user_operation_view (id, user_id, operation_type, symbol, amount_ars)
VALUES
  ('40000000-0000-0000-0000-000000000001', 'user-demo-1', 'DEPOSIT', NULL, 5000000.00)
ON CONFLICT (id) DO NOTHING;

