CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    sku VARCHAR(80) UNIQUE,
    barcode VARCHAR(80) UNIQUE,
    unit VARCHAR(30) NOT NULL DEFAULT 'UN',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_items (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    quantity NUMERIC(14,3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_stock_item_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT uk_stock_item_product
        UNIQUE (product_id),

    CONSTRAINT ck_stock_quantity
        CHECK (quantity >= 0)
);

CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    batch_code VARCHAR(100),
    expiration_date DATE,
    quantity NUMERIC(14,3) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_batch_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT ck_batch_quantity
        CHECK (quantity >= 0)
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT,
    type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    assigned_user_id BIGINT,
    created_by_user_id BIGINT,
    due_date TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_assigned_user
        FOREIGN KEY (assigned_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_task_created_by_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id)
);

CREATE TABLE task_items (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    product_id BIGINT,
    expected_quantity NUMERIC(14,3),
    actual_quantity NUMERIC(14,3),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_task_item_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_item_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
);

CREATE TABLE inventory_counts (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    created_by_user_id BIGINT,
    assigned_user_id BIGINT,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_count_created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_inventory_count_assigned_user
        FOREIGN KEY (assigned_user_id)
        REFERENCES users(id)
);

CREATE TABLE inventory_count_items (
    id BIGSERIAL PRIMARY KEY,
    inventory_count_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    expected_quantity NUMERIC(14,3) NOT NULL DEFAULT 0,
    counted_quantity NUMERIC(14,3),
    difference_quantity NUMERIC(14,3),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inventory_count_item_count
        FOREIGN KEY (inventory_count_id)
        REFERENCES inventory_counts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_inventory_count_item_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
);

CREATE TABLE damages (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    batch_id BIGINT,
    quantity NUMERIC(14,3) NOT NULL,
    reason VARCHAR(160),
    description TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    reported_by_user_id BIGINT,
    approved_by_user_id BIGINT,
    reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,

    CONSTRAINT fk_damage_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT fk_damage_batch
        FOREIGN KEY (batch_id)
        REFERENCES batches(id),

    CONSTRAINT fk_damage_reported_by
        FOREIGN KEY (reported_by_user_id)
        REFERENCES users(id),

    CONSTRAINT fk_damage_approved_by
        FOREIGN KEY (approved_by_user_id)
        REFERENCES users(id),

    CONSTRAINT ck_damage_quantity
        CHECK (quantity > 0)
);

CREATE TABLE receivings (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(80),
    invoice_access_key VARCHAR(80),
    supplier_name VARCHAR(160),
    supplier_document VARCHAR(30),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING',
    received_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_receiving_user
        FOREIGN KEY (received_by_user_id)
        REFERENCES users(id)
);

CREATE TABLE receiving_items (
    id BIGSERIAL PRIMARY KEY,
    receiving_id BIGINT NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(160) NOT NULL,
    barcode VARCHAR(80),
    expected_quantity NUMERIC(14,3) NOT NULL DEFAULT 0,
    received_quantity NUMERIC(14,3),
    batch_code VARCHAR(100),
    expiration_date DATE,
    quality_status VARCHAR(40),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_receiving_item_receiving
        FOREIGN KEY (receiving_id)
        REFERENCES receivings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_receiving_item_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
);

CREATE INDEX idx_products_name
    ON products(name);

CREATE INDEX idx_products_barcode
    ON products(barcode);

CREATE INDEX idx_batches_expiration_date
    ON batches(expiration_date);

CREATE INDEX idx_tasks_status
    ON tasks(status);

CREATE INDEX idx_tasks_assigned_user
    ON tasks(assigned_user_id);

CREATE INDEX idx_inventory_counts_status
    ON inventory_counts(status);

CREATE INDEX idx_damages_status
    ON damages(status);

CREATE INDEX idx_receivings_status
    ON receivings(status);