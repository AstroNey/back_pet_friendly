-- RBAC : ajoute le rôle utilisateur (base du panel admin).
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';