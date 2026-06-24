-- Modération des avis : statut + traçabilité.
-- Grandfather : tous les avis existants sont considérés déjà validés → APPROVED
-- (pas de re-modération du passé). Les nouveaux avis seront créés en PENDING par l'app.

ALTER TABLE reviews ADD COLUMN status VARCHAR(20);
UPDATE reviews SET status = 'APPROVED' WHERE status IS NULL;
ALTER TABLE reviews ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE reviews ALTER COLUMN status SET NOT NULL;

ALTER TABLE reviews ADD COLUMN moderated_at TIMESTAMPTZ;
ALTER TABLE reviews ADD COLUMN moderated_by UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_reviews_status       ON reviews(status);
CREATE INDEX idx_reviews_place_status ON reviews(place_id, status);
