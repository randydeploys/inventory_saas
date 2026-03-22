-- ============================================================
-- V3 — Product, ProductStock, StockMovement
-- ============================================================

-- Enums
CREATE TYPE tracking_type AS ENUM ('QUANTITY', 'UNIQUE');
CREATE TYPE movement_type AS ENUM ('IN', 'OUT', 'TRANSFER');

-- ============================================================
-- PRODUCT
-- ============================================================
CREATE TABLE product (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID           NOT NULL REFERENCES tenant(id),
    category_id     UUID           REFERENCES category(id),
    name            VARCHAR(255)   NOT NULL,
    sku             VARCHAR(100)   NOT NULL,
    description     TEXT,
    tracking_type   tracking_type  NOT NULL,
    serial_number   VARCHAR(255),
    min_quantity    INTEGER,
    unit            VARCHAR(50),
    updated_by      UUID           REFERENCES users(id),
    deleted_at      TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_product_tenant_sku
    ON product(tenant_id, sku)
    WHERE deleted_at IS NULL;

-- ============================================================
-- PRODUCT_STOCK
-- ============================================================
CREATE TABLE product_stock (
    id          UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID      NOT NULL REFERENCES tenant(id),
    product_id  UUID      NOT NULL REFERENCES product(id),
    room_id     UUID      NOT NULL REFERENCES room(id),
    quantity    INTEGER   NOT NULL CHECK (quantity > 0),
    CONSTRAINT uq_product_stock_product_room UNIQUE (product_id, room_id)
);

-- ============================================================
-- STOCK_MOVEMENT
-- ============================================================
CREATE TABLE stock_movement (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL REFERENCES tenant(id),
    product_id      UUID          NOT NULL REFERENCES product(id),
    from_room_id    UUID          REFERENCES room(id),
    to_room_id      UUID          REFERENCES room(id),
    quantity        INTEGER       NOT NULL CHECK (quantity > 0),
    type            movement_type NOT NULL,
    reason          TEXT,
    performed_by    UUID          NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEX
-- ============================================================
CREATE INDEX idx_product_tenant_id          ON product(tenant_id);
CREATE INDEX idx_product_category_id        ON product(category_id);
CREATE INDEX idx_product_deleted_at         ON product(deleted_at) WHERE deleted_at IS NULL;

CREATE INDEX idx_product_stock_tenant_id    ON product_stock(tenant_id);
CREATE INDEX idx_product_stock_product_id   ON product_stock(product_id);
CREATE INDEX idx_product_stock_room_id      ON product_stock(room_id);

CREATE INDEX idx_stock_movement_tenant_id   ON stock_movement(tenant_id);
CREATE INDEX idx_stock_movement_product_id  ON stock_movement(product_id);
CREATE INDEX idx_stock_movement_performed   ON stock_movement(performed_by);
CREATE INDEX idx_stock_movement_created_at  ON stock_movement(created_at);
CREATE INDEX idx_stock_movement_type ON stock_movement(type);
