CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    phone       VARCHAR(30),
    avatar_url  VARCHAR(500),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE pets (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    species    VARCHAR(20)  NOT NULL,
    breed      VARCHAR(100),
    age_years  INT,
    weight_kg  DOUBLE PRECISION,
    photo_url  VARCHAR(500),
    notes      TEXT,
    owner_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE places (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    category             VARCHAR(30)  NOT NULL,
    address              VARCHAR(255) NOT NULL,
    city                 VARCHAR(100),
    postal_code          VARCHAR(20),
    country              VARCHAR(100),
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    phone                VARCHAR(30),
    website              VARCHAR(500),
    photo_url            VARCHAR(500),
    pet_water_available  BOOLEAN NOT NULL DEFAULT FALSE,
    pet_menu_available   BOOLEAN NOT NULL DEFAULT FALSE,
    indoor_pets_allowed  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE reviews (
    id         BIGSERIAL PRIMARY KEY,
    rating     INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    place_id   BIGINT NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    author_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE (place_id, author_id)
);

CREATE INDEX idx_pets_owner      ON pets(owner_id);
CREATE INDEX idx_reviews_place   ON reviews(place_id);
CREATE INDEX idx_reviews_author  ON reviews(author_id);
CREATE INDEX idx_places_city     ON places(city);
CREATE INDEX idx_places_category ON places(category);
