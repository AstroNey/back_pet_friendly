# ADR 0003 — Refresh token rotation avec hash SHA-256 en DB

- **Date** : 2026-04-22
- **Status** : Accepted

## Context

Auth JWT classique : access token courte durée (15 min) + refresh token longue durée (7 j). Question : que faire si un refresh token est volé ?

Options envisagées :
1. Rien — révocation impossible, l'attaquant l'utilise pendant 7 j.
2. Blacklist DB — stocker tous les tokens révoqués jusqu'à expiration.
3. **Rotation à chaque usage + hash en DB** (choix retenu).

## Decision

À chaque login/register/refresh :
1. Générer access JWT (15 min) + refresh JWT (7 j).
2. **Hasher SHA-256 le refresh** et persister `RefreshToken{userId, tokenHash, expiresAt, revokedAt}`.
3. Retourner les deux tokens en clair au client.

Sur `/auth/refresh` :
1. Valider la signature JWT.
2. Lookup le hash en DB → vérifier `isActive()` (non révoqué, non expiré).
3. **Marquer l'ancien `revokedAt = now`** et émettre nouvelle paire (rotation).

Sur `/auth/logout` : marquer `revokedAt` directement.

Implémentation : `domain/service/AuthService.java`, `domain/service/TokenHasher.java`, table `refresh_tokens`.

## Consequences

**Positif**
- Refresh volé → inutile dès que le légitime utilisateur s'en sert (replay détecté côté DB lors de la rotation).
- Logout effectif côté serveur (pas juste front-side).
- Coût modeste : 1 row par session active, cleanable par job de purge sur `expires_at`.

**Négatif**
- Stateful : la DB doit être disponible pour valider. Vrai trade-off vs JWT pur stateless.
- Une race condition possible (deux refreshes parallèles) : le second échoue, à gérer côté client (retry après login).
- TTL hardcodé dans `AuthService` (`ACCESS_TOKEN_TTL_SECONDS = 900`, `REFRESH_TOKEN_TTL_DAYS = 7`) — devrait migrer vers `application.yml` si besoin de variabilité par env.

**Note sécurité**
- SHA-256 sans sel : OK car le refresh token lui-même est un secret aléatoire long (entropie suffisante). Pas besoin de bcrypt/argon ici.
- Le JWT est signé HS256 ; ne jamais y mettre de données sensibles (lisible base64).
