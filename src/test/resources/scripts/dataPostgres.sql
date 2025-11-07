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
    ('aaaa1111-0000-4000-b111-000000000001', 'FR761234567890', 2500.00, NOW(), 'CREATED',
     '11111111-aaaa-4aaa-aaaa-111111111111',
     'CURRENT ACCOUNT', 500.00, NULL),

    ('bbbb2222-0000-4000-b222-000000000002', 'FR769876543210', 1500.00, NOW(), 'CREATED',
     '22222222-bbbb-4bbb-bbbb-222222222222',
     'CURRENT ACCOUNT', 300.00, NULL),

    ('cccc3333-0000-4000-b333-000000000003', 'FR761112223334', 3200.00, NOW(), 'CREATED',
     '33333333-cccc-4ccc-cccc-333333333333',
     'CURRENT ACCOUNT', 400.00, NULL);

-- ==========================
-- INSERT INTO OPERATION
-- ==========================
INSERT INTO operation (operation_id, operation_number, operation_amount, operation_date, operation_type, account_id, description)
VALUES
    ('11111111-0000-4000-b111-000000000001', 'OP-20231010-000001', 200.00, NOW(), 'DEBIT',
     'aaaa1111-0000-4000-b111-000000000001', 'Grocery shopping'),

    ('22222222-0000-4000-b222-000000000002', 'OP-20231010-000002', 500.00, NOW(), 'CREDIT',
     'aaaa1111-0000-4000-b111-000000000001', 'Salary deposit'),

    ('33333333-0000-4000-b333-000000000003', 'OP-20231010-000003', 150.00, NOW(), 'DEBIT',
     'cccc3333-0000-4000-b333-000000000003', 'Restaurant payment'),

    ('44444444-0000-4000-b444-000000000004', 'OP-20231010-000004', 1200.00, NOW(), 'CREDIT',
     'cccc3333-0000-4000-b333-000000000003', 'Project freelance payment');
