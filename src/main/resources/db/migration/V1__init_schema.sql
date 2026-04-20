-- Pet-Friendly initial schema (PostgreSQL + PostGIS)

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- ─── Users ──────────────────────────────────────────────────────────────
CREATE TABLE users (
    id             UUID PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    avatar_url     VARCHAR(500),
    fcm_token      VARCHAR(512),
    pets           TEXT,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_users_email ON users(email);

-- ─── Places ─────────────────────────────────────────────────────────────
CREATE TABLE places (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(30)  NOT NULL,
    address       VARCHAR(500) NOT NULL,
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION,
    location      GEOGRAPHY(POINT, 4326),
    rating        DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    review_count  INTEGER          NOT NULL DEFAULT 0,
    animals       TEXT,
    image_url     VARCHAR(500),
    gallery_urls  TEXT,
    description   TEXT,
    hours         TEXT,
    owner_id      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_places_type     ON places(type);
CREATE INDEX idx_places_location ON places USING GIST(location);
CREATE INDEX idx_places_name     ON places USING GIN(to_tsvector('french', name));

-- ─── Reviews ────────────────────────────────────────────────────────────
CREATE TABLE reviews (
    id                 UUID PRIMARY KEY,
    place_id           UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    author_id          UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    author_name        VARCHAR(255),
    author_avatar_url  VARCHAR(500),
    rating             DOUBLE PRECISION NOT NULL CHECK (rating >= 0 AND rating <= 5),
    text               TEXT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_author_place UNIQUE (place_id, author_id)
);
CREATE INDEX idx_reviews_place   ON reviews(place_id);
CREATE INDEX idx_reviews_author  ON reviews(author_id);

-- ─── Favorites ──────────────────────────────────────────────────────────
CREATE TABLE favorites (
    user_id     UUID NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    place_id    UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, place_id)
);
CREATE INDEX idx_favorites_user ON favorites(user_id);

-- ─── Refresh tokens ─────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ─── Notifications ──────────────────────────────────────────────────────
CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    body        VARCHAR(1000),
    read        BOOLEAN NOT NULL DEFAULT FALSE,
    payload     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);
