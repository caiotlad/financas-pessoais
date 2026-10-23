CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_accounts_owner_name UNIQUE (owner_id, name),
    CONSTRAINT uq_accounts_owner_id UNIQUE (owner_id, id)
);

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    CONSTRAINT uq_categories_owner_name_direction UNIQUE (owner_id, name, direction),
    CONSTRAINT uq_categories_owner_id UNIQUE (owner_id, id)
);

CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    account_id UUID NOT NULL,
    category_id UUID NOT NULL,
    direction VARCHAR(10) NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    description VARCHAR(240) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_transaction_account_owner FOREIGN KEY (owner_id, account_id) REFERENCES accounts(owner_id, id),
    CONSTRAINT fk_transaction_category_owner FOREIGN KEY (owner_id, category_id) REFERENCES categories(owner_id, id)
);

CREATE INDEX idx_transactions_owner_date ON ledger_transactions(owner_id, occurred_at DESC);
CREATE INDEX idx_transactions_owner_account_date ON ledger_transactions(owner_id, account_id, occurred_at DESC);
CREATE INDEX idx_transactions_owner_category ON ledger_transactions(owner_id, category_id);
