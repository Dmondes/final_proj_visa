-- Add price_alerts column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS price_alerts TEXT;

-- Add fcm_token column if it doesn't exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS fcm_token TEXT;

-- Update any existing rows with null values
UPDATE users SET price_alerts = '' WHERE price_alerts IS NULL;
UPDATE users SET fcm_token = NULL WHERE fcm_token IS NULL;