-- ==========================
-- CLEANUP (le schéma est géré par Liquibase, ce script ne gère que les données)
-- Ordre FK-safe : lignes de relevés -> relevés -> opérations -> comptes -> clients
-- ==========================
DELETE FROM statement_line;
DELETE FROM bank_statement;
DELETE FROM operation;
DELETE FROM account;
DELETE FROM customer;

-- ==========================
-- INSERT INTO CUSTOMER
-- ==========================
INSERT INTO customer (customer_id, first_name, last_name, email, gender)
VALUES
    ('11111111-aaaa-4aaa-aaaa-111111111111', 'John', 'Doe', 'john.doe@example.com', 'MALE'),
    ('22222222-bbbb-4bbb-bbbb-222222222222', 'Jane', 'Smith', 'jane.smith@example.com', 'FEMALE'),
    ('33333333-cccc-4ccc-cccc-333333333333', 'Alexandre', 'Durant', 'alexandre.durant@google.com', 'MALE'),
    ('33333333-cccc-4ccc-cccc-333332313445', 'Souleymane', 'BARRY', 'souleymane.barry@gmail.com', 'MALE');

-- ==========================
-- INSERT INTO ACCOUNT
-- ==========================
INSERT INTO account (account_id, rib, balance, created_at, status, customer_id, account_type, over_draft, interest_rate)
VALUES
    ('aaaa1111-0000-4000-b111-000000000001', 'FR761234567890', 2500.00, '2025-11-09T03:06:56', 'CREATED',
     '11111111-aaaa-4aaa-aaaa-111111111111',
     'CURRENT ACCOUNT', 500.00, NULL),

    ('bbbb2222-0000-4000-b222-000000000002', 'FR769876543210', 1500.00, '2025-11-09T03:06:56', 'CREATED',
     '22222222-bbbb-4bbb-bbbb-222222222222',
     'CURRENT ACCOUNT', 300.00, NULL),

    ('cccc3333-0000-4000-b333-000000000003', 'FR761112223334', 3200.00, '2025-11-09T03:06:56', 'CREATED',
     '33333333-cccc-4ccc-cccc-333333333333',
     'CURRENT ACCOUNT', 400.00, NULL);

-- ==========================
-- INSERT INTO OPERATION
-- ==========================
INSERT INTO operation (operation_id, operation_number, operation_amount, operation_date, operation_type, account_id, description)
VALUES
    ('11111111-0000-4000-b111-000000000001', 'OP-20231010-000001', 200.00, '2025-11-09T03:06:56', 'DEBIT',
     'aaaa1111-0000-4000-b111-000000000001', 'Grocery shopping'),

    ('22222222-0000-4000-b222-000000000002', 'OP-20231010-000002', 500.00, '2025-11-09T03:06:56', 'CREDIT',
     'aaaa1111-0000-4000-b111-000000000001', 'Salary deposit'),

    ('33333333-0000-4000-b333-000000000003', 'OP-20231010-000003', 150.00, '2025-11-09T03:06:56', 'DEBIT',
     'cccc3333-0000-4000-b333-000000000003', 'Restaurant payment'),

    ('44444444-0000-4000-b444-000000000004', 'OP-20231010-000004', 1200.00, '2025-11-09T03:06:56', 'CREDIT',
     'cccc3333-0000-4000-b333-000000000003', 'Project freelance payment');

-- ===================================================================
-- Données dédiées à l'observation du N+1 pour le client 33333333-...-333.
-- 2 comptes : le compte COURANT existant (cccc3333) + un compte ÉPARGNE
-- (cccc4444), chacun avec >= 20 opérations.
--   getFullCustomerData  : 1 requête (comptes) + 1 requête PAR compte -> N+1
--   getFullCustomerData2 : 1 seule requête d'opérations (clause IN)
-- ===================================================================

-- Compte ÉPARGNE du client 333 (interest_rate renseigné, over_draft NULL)
INSERT INTO account (account_id, rib, balance, created_at, status, customer_id, account_type, over_draft, interest_rate)
VALUES
    ('cccc4444-0000-4000-b333-000000000031', 'FR761112223335', 10000.00, '2025-11-09T03:06:56', 'CREATED',
     '33333333-cccc-4ccc-cccc-333333333333',
     'SAVING ACCOUNT', NULL, 2.50);

-- 18 opérations supplémentaires sur le compte COURANT cccc3333 (2 existantes -> 20 au total)
INSERT INTO operation (operation_id, operation_number, operation_amount, operation_date, operation_type, account_id, description)
VALUES
    ('cccc3333-0000-4000-b901-000000000001', 'OP-CUR-0001', 10.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 1'),
    ('cccc3333-0000-4000-b901-000000000002', 'OP-CUR-0002', 20.00,  '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 2'),
    ('cccc3333-0000-4000-b901-000000000003', 'OP-CUR-0003', 30.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 3'),
    ('cccc3333-0000-4000-b901-000000000004', 'OP-CUR-0004', 40.00,  '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 4'),
    ('cccc3333-0000-4000-b901-000000000005', 'OP-CUR-0005', 50.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 5'),
    ('cccc3333-0000-4000-b901-000000000006', 'OP-CUR-0006', 60.00,  '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 6'),
    ('cccc3333-0000-4000-b901-000000000007', 'OP-CUR-0007', 70.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 7'),
    ('cccc3333-0000-4000-b901-000000000008', 'OP-CUR-0008', 80.00,  '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 8'),
    ('cccc3333-0000-4000-b901-000000000009', 'OP-CUR-0009', 90.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 9'),
    ('cccc3333-0000-4000-b901-000000000010', 'OP-CUR-0010', 100.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 10'),
    ('cccc3333-0000-4000-b901-000000000011', 'OP-CUR-0011', 110.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 11'),
    ('cccc3333-0000-4000-b901-000000000012', 'OP-CUR-0012', 120.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 12'),
    ('cccc3333-0000-4000-b901-000000000013', 'OP-CUR-0013', 130.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 13'),
    ('cccc3333-0000-4000-b901-000000000014', 'OP-CUR-0014', 140.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 14'),
    ('cccc3333-0000-4000-b901-000000000015', 'OP-CUR-0015', 150.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 15'),
    ('cccc3333-0000-4000-b901-000000000016', 'OP-CUR-0016', 160.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 16'),
    ('cccc3333-0000-4000-b901-000000000017', 'OP-CUR-0017', 170.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc3333-0000-4000-b333-000000000003', 'Current operation 17'),
    ('cccc3333-0000-4000-b901-000000000018', 'OP-CUR-0018', 180.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc3333-0000-4000-b333-000000000003', 'Current operation 18');

-- 20 opérations sur le compte ÉPARGNE cccc4444
INSERT INTO operation (operation_id, operation_number, operation_amount, operation_date, operation_type, account_id, description)
VALUES
    ('cccc4444-0000-4000-b902-000000000001', 'OP-SAV-0001', 25.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 1'),
    ('cccc4444-0000-4000-b902-000000000002', 'OP-SAV-0002', 50.00,  '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 2'),
    ('cccc4444-0000-4000-b902-000000000003', 'OP-SAV-0003', 75.00,  '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 3'),
    ('cccc4444-0000-4000-b902-000000000004', 'OP-SAV-0004', 100.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 4'),
    ('cccc4444-0000-4000-b902-000000000005', 'OP-SAV-0005', 125.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 5'),
    ('cccc4444-0000-4000-b902-000000000006', 'OP-SAV-0006', 150.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 6'),
    ('cccc4444-0000-4000-b902-000000000007', 'OP-SAV-0007', 175.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 7'),
    ('cccc4444-0000-4000-b902-000000000008', 'OP-SAV-0008', 200.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 8'),
    ('cccc4444-0000-4000-b902-000000000009', 'OP-SAV-0009', 225.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 9'),
    ('cccc4444-0000-4000-b902-000000000010', 'OP-SAV-0010', 250.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 10'),
    ('cccc4444-0000-4000-b902-000000000011', 'OP-SAV-0011', 275.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 11'),
    ('cccc4444-0000-4000-b902-000000000012', 'OP-SAV-0012', 300.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 12'),
    ('cccc4444-0000-4000-b902-000000000013', 'OP-SAV-0013', 325.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 13'),
    ('cccc4444-0000-4000-b902-000000000014', 'OP-SAV-0014', 350.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 14'),
    ('cccc4444-0000-4000-b902-000000000015', 'OP-SAV-0015', 375.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 15'),
    ('cccc4444-0000-4000-b902-000000000016', 'OP-SAV-0016', 400.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 16'),
    ('cccc4444-0000-4000-b902-000000000017', 'OP-SAV-0017', 425.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 17'),
    ('cccc4444-0000-4000-b902-000000000018', 'OP-SAV-0018', 450.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 18'),
    ('cccc4444-0000-4000-b902-000000000019', 'OP-SAV-0019', 475.00, '2025-11-09T03:06:56', 'DEBIT',  'cccc4444-0000-4000-b333-000000000031', 'Saving operation 19'),
    ('cccc4444-0000-4000-b902-000000000020', 'OP-SAV-0020', 500.00, '2025-11-09T03:06:56', 'CREDIT', 'cccc4444-0000-4000-b333-000000000031', 'Saving operation 20');
