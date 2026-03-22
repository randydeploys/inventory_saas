-- ============================================================
-- V2 — Catégories de produits
-- ============================================================

CREATE TABLE category (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenant(id),
    name        VARCHAR(255) NOT NULL,
    color       VARCHAR(7)   NOT NULL,
    updated_by  UUID         REFERENCES users(id),
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index : filtrage par tenant (le plus courant)
CREATE INDEX idx_category_tenant_id  ON category(tenant_id);
-- Index partiel : on liste surtout les catégories actives
CREATE INDEX idx_category_deleted_at ON category(deleted_at) WHERE deleted_at IS NULL;
