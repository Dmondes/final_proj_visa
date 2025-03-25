-- Add price_alerts column to users table
ALTER TABLE users ADD COLUMN price_alerts TEXT DEFAULT '';

-- Add fcm_token column to users table
ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255) DEFAULT NULL;