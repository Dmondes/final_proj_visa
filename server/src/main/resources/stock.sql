DROP DATABASE IF EXISTS stocks;

-- Create a new database
CREATE DATABASE stocks;

-- Use the new database
USE stocks;

-- Create the listing table, symbol as primary key
CREATE TABLE listing (
    symbol VARCHAR(10) PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    market_cap DOUBLE,
    ipo_year INT,
    volume DOUBLE,
    sector VARCHAR(100),
    industry VARCHAR(100)
);

-- CREATE TABLE users (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     email VARCHAR(255) UNIQUE NOT NULL,
--     password VARCHAR(255) NOT NULL,
--     watchlist TEXT
-- );
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    watchlist TEXT,
    price_alerts TEXT,
    fcm_token TEXT
);

-- -- Add price_alerts column to users table
-- ALTER TABLE users ADD COLUMN price_alerts TEXT DEFAULT '';
-- -- Add fcm_token column to users table
-- ALTER TABLE users ADD COLUMN fcm_token VARCHAR(255) DEFAULT NULL;
