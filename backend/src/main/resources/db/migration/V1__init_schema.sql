-- ============================================================
-- V1 — Schéma initial : Tenant, User, Building, Room, RefreshToken
-- ============================================================

-- Enum Role
CREATE TYPE user_role AS ENUM ('ADMIN', 'MANAGER', 'READER');

-- ============================================================
-- TENANT
-- ============================================================
CREATE TABLE tenant (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ============================================================
-- USERS  (éviter le mot réservé "user")
-- ============================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenant(id),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          user_role    NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

-- ============================================================
-- BUILDING
-- ============================================================
CREATE TABLE building (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenant(id),
    name        VARCHAR(255) NOT NULL,
    address     TEXT,
    updated_by  UUID         REFERENCES users(id),
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- ROOM
-- ============================================================
CREATE TABLE room (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL REFERENCES tenant(id),
    building_id UUID         NOT NULL REFERENCES building(id),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    updated_by  UUID         REFERENCES users(id),
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- REFRESH_TOKEN
-- ============================================================
CREATE TABLE refresh_token (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id),
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEX
-- ============================================================
CREATE INDEX idx_users_tenant_id        ON users(tenant_id);
CREATE INDEX idx_building_tenant_id     ON building(tenant_id);
CREATE INDEX idx_building_deleted_at    ON building(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_room_tenant_id         ON room(tenant_id);
CREATE INDEX idx_room_building_id       ON room(building_id);
CREATE INDEX idx_room_deleted_at        ON room(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_refresh_token_user_id  ON refresh_token(user_id);
