CREATE TABLE accounts
(
    id            BIGSERIAL PRIMARY KEY,
    public_id     UUID         NOT NULL DEFAULT gen_random_uuid(),
    owner_name    VARCHAR(50)  NOT NULL,
    type          VARCHAR(20)  NOT NULL,
    currency      VARCHAR(3)   NOT NULL DEFAULT 'INR',
    balance_paise BIGINT       NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_accounts_public_id UNIQUE (public_id),
    CONSTRAINT ck_accounts_type CHECK ( type IN ('CUSTOMER', 'MERCHANT', 'SYSTEM_FUNDING') ),
    CONSTRAINT ck_accounts_currency CHECK ( currency = 'INR' ),
    CONSTRAINT ck_accounts_status CHECK ( status IN ('ACTIVE', 'FROZEN') ),
    CONSTRAINT ck_accounts_negative_balance CHECK (
        balance_paise >= 0 OR type = 'SYSTEM_FUNDING'
    )
);

INSERT INTO accounts (owner_name, type)
VALUES ('Ledgerline Funding', 'SYSTEM_FUNDING');
