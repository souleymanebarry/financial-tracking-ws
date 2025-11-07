-- ==========================
-- DROP EXISTING TABLES (for safety)
-- ==========================
DROP TABLE IF EXISTS operation CASCADE;
DROP TABLE IF EXISTS account CASCADE;
DROP TABLE IF EXISTS customer CASCADE;

-- ==========================
-- CUSTOMER
-- ==========================
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE customer(
    customer_id UUID PRIMARY KEY NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(150) UNIQUE NOT NULL,
    gender VARCHAR(10)
);

-- ==========================
-- ACCOUNT (Single Table Inheritance)
-- ==========================

CREATE TABLE account(
  account_id UUID PRIMARY KEY NOT NULL,
  rib VARCHAR(50) UNIQUE NOT NULL,
  balance NUMERIC(19,2),
  created_at TIMESTAMP,
  status VARCHAR(20),
  customer_id UUID CONSTRAINT fknnwpo0lfq4xai1rs6887sx02k REFERENCES customer,
  account_type VARCHAR(30) NOT NULL,
  over_draft NUMERIC(38, 2),
  interest_rate NUMERIC(38, 2)
);

-- ==========================
-- OPERATION
-- ==========================

CREATE TABLE operation (
  operation_id UUID PRIMARY KEY NOT NULL,
  operation_number VARCHAR(255),
  operation_amount NUMERIC(38, 2),
  operation_date TIMESTAMP,
  operation_type VARCHAR(20),
  account_id UUID CONSTRAINT fkloy20r01mn4truqqu460w3j9q REFERENCES account,
  description VARCHAR(255)
);
