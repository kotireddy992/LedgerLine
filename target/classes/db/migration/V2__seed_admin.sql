-- V2__seed_admin.sql
-- Seeds one ADMIN user for initial system access.
-- Password: Admin@1234  (BCrypt hash below, cost=12)
-- Change this password immediately after first login in production.

INSERT INTO users (name, email, password_hash, role)
VALUES (
    'System Admin',
    'admin@ledgerline.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'ADMIN'
);
