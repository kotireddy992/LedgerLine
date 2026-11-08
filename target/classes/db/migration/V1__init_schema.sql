-- V1__init_schema.sql
-- LedgerLine initial schema
-- All monetary columns use DECIMAL(19,2) — never FLOAT or DOUBLE.
-- All timestamps use DATETIME(6) in UTC.

CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role         ENUM('ADMIN','ACCOUNT_HOLDER','AUDITOR') NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE accounts (
    id             BIGINT          NOT NULL AUTO_INCREMENT,
    account_number VARCHAR(20)     NOT NULL,
    balance        DECIMAL(19,2)   NOT NULL DEFAULT 0.00,
    currency       CHAR(3)         NOT NULL DEFAULT 'INR',
    status         ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
    owner_id       BIGINT          NOT NULL,
    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version        BIGINT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_accounts_number (account_number),
    INDEX idx_accounts_owner (owner_id),
    INDEX idx_accounts_number (account_number),
    CONSTRAINT fk_accounts_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE transfers (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    source_account_id      BIGINT        NOT NULL,
    destination_account_id BIGINT        NOT NULL,
    amount                 DECIMAL(19,2) NOT NULL,
    currency               CHAR(3)       NOT NULL,
    status                 ENUM('PENDING','COMPLETED','FAILED','REVERSED') NOT NULL,
    idempotency_key        VARCHAR(100)  NOT NULL,
    initiated_by_id        BIGINT        NOT NULL,
    failure_reason         VARCHAR(500)  NULL,
    created_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_transfers_idempotency (idempotency_key),
    INDEX idx_transfers_source (source_account_id),
    INDEX idx_transfers_dest (destination_account_id),
    INDEX idx_transfers_initiated_by (initiated_by_id),
    CONSTRAINT fk_transfers_source   FOREIGN KEY (source_account_id)      REFERENCES accounts(id),
    CONSTRAINT fk_transfers_dest     FOREIGN KEY (destination_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transfers_initiator FOREIGN KEY (initiated_by_id)       REFERENCES users(id),
    CONSTRAINT chk_transfers_different_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT chk_transfers_positive_amount    CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ledger_entries (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    transaction_reference VARCHAR(100)  NOT NULL,
    account_id            BIGINT        NOT NULL,
    entry_type            ENUM('DEBIT','CREDIT') NOT NULL,
    amount                DECIMAL(19,2) NOT NULL,
    resulting_balance     DECIMAL(19,2) NOT NULL,
    initiated_by_id       BIGINT        NOT NULL,
    created_at            DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ledger_account (account_id),
    INDEX idx_ledger_tx_ref (transaction_reference),
    INDEX idx_ledger_created_at (created_at),
    CONSTRAINT fk_ledger_account   FOREIGN KEY (account_id)      REFERENCES accounts(id),
    CONSTRAINT fk_ledger_initiator FOREIGN KEY (initiated_by_id) REFERENCES users(id),
    CONSTRAINT chk_ledger_positive_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE idempotency_records (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    response_body   TEXT         NULL,
    response_status INT          NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_idempotency_key (idempotency_key),
    INDEX idx_idempotency_key (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
