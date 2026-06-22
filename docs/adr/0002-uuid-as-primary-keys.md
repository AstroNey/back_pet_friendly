# ADR 0002 — UUID comme clé primaire partout

- **Date** : 2026-04-20
- **Status** : Accepted

## Context

Choix entre `Long` auto-incrémenté ou `UUID` v4 pour les IDs. La spec `BACKEND_JAVA_SPRING.md` impose UUID. Décision à valider et acter.

## Decision

UUID v4 pour TOUS les IDs (User, Place, Review, Notification, RefreshToken, etc.). Stocké natif PostgreSQL (`columnDefinition = "uuid"`), généré côté domaine (`UUID.randomUUID()`) avant `save`.

## Consequences

**Positif**
- IDs générables sans round-trip DB (utile dans les services, les tests, le seed data).
- Pas de conflit de séquence en cas de merge multi-DB ou réplication.
- IDs non-prédictibles → pas de risque d'IDOR par incrémentation.
- Compatible PostGIS, indexable correctement.

**Négatif**
- 16 octets vs 8 octets (Long) — taille des index x2.
- Moins lisibles dans les logs (`a3f...` vs `42`).
- Pas d'ordre temporel implicite (utiliser UUIDv7 si besoin un jour).

**À garder en tête**
- Côté front, manipuler les UUIDs en string (Java→JSON serialize natif).
- Migrations SQL : utiliser `uuid_generate_v4()` ou recevoir l'UUID du backend.
