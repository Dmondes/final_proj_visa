DROP DATABASE IF EXISTS stocks;

-- Create a new database
CREATE DATABASE stocks;

-- Use the new database
USE stocks;

-- Create user called 'finbro'
CREATE USER IF NOT EXISTS 'finbro'@'%' IDENTIFIED BY 'password123';

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

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    watchlist TEXT
);

-- Grant privileges to user 'finbro'
GRANT ALL PRIVILEGES ON stocks.* TO 'finbro'@'%';
FLUSH PRIVILEGES;
