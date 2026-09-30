ALTER TABLE tasks ADD COLUMN execution_notes TEXT;
CREATE TABLE inventory_imports (
    id BIGSERIAL PRIMARY KEY,
    request_key UUID NOT NULL UNIQUE,
    payload_hash VARCHAR(64) NOT NULL,
    file_name VARCHAR(200) NOT NULL,
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    inventory_count_id BIGINT REFERENCES inventory_counts(id),
    product_count INTEGER NOT NULL,
    created_products INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_inventory_counts_assignee ON inventory_counts(assigned_user_id);
CREATE INDEX idx_users_email_lower ON users(lower(email));
