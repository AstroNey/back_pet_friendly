-- H6 : la colonne `location` (GEOGRAPHY) n'était jamais peuplée — l'entité ne mappe que
-- latitude/longitude et `searchNearby` recalculait ST_MakePoint à la volée dans le WHERE/ORDER BY,
-- ce qui empêche l'usage de l'index GIST (seq scan + ST_Distance sur toute la table).
--
-- On la redéfinit en colonne GÉNÉRÉE STORED dérivée de longitude/latitude : Postgres la maintient
-- automatiquement à chaque INSERT/UPDATE (fonctions immutables), et l'index GIST redevient exploitable
-- par ST_DWithin. Aucune écriture applicative sur cette colonne (l'entité ne la mappe pas).

DROP INDEX IF EXISTS idx_places_location;
ALTER TABLE places DROP COLUMN IF EXISTS location;

ALTER TABLE places
    ADD COLUMN location GEOGRAPHY(POINT, 4326)
    GENERATED ALWAYS AS (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography) STORED;

CREATE INDEX idx_places_location ON places USING GIST(location);
