-- Development only. Shared test password: TestSales2026!
-- Run manually after Flyway has created the schema. Never use in production.
BEGIN;

INSERT INTO sales_user (username, name, email, password_hash, role, active)
VALUES
    ('test-admin', 'Test Administrator', 'test-admin@e-sales.example.test',
     '$2y$12$8wIw1F1MlT6FWX9h2VpV7uatYeymFbwFJSOSULGO5a6oB2S3ppuue',
     'PLATFORM_ADMIN', TRUE),
    ('test-alex', 'Alex Martin', 'test-alex@e-sales.example.test',
     '$2y$12$8wIw1F1MlT6FWX9h2VpV7uatYeymFbwFJSOSULGO5a6oB2S3ppuue',
     'SALES_USER', TRUE),
    ('test-samira', 'Samira Benali', 'test-samira@e-sales.example.test',
     '$2y$12$8wIw1F1MlT6FWX9h2VpV7uatYeymFbwFJSOSULGO5a6oB2S3ppuue',
     'SALES_USER', TRUE),
    ('test-julien', 'Julien Dupont', 'test-julien@e-sales.example.test',
     '$2y$12$8wIw1F1MlT6FWX9h2VpV7uatYeymFbwFJSOSULGO5a6oB2S3ppuue',
     'SALES_USER', TRUE)
ON CONFLICT (username) DO NOTHING;

COMMIT;

SELECT id, username, name, email, role, active
FROM sales_user
WHERE username IN ('test-admin', 'test-alex', 'test-samira', 'test-julien')
ORDER BY username;
