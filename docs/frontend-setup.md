# Setup côté front — panel admin Next.js relié à ce backend

Front cible : **panel admin interne en Next.js (App Router) + Refine**, extensible selon les demandes.
Le jour où tu crées le projet front, copie le bloc ci-dessous dans son **`CLAUDE.md`** racine. Toute session Claude côté front aura le contexte minimal pour appeler ce backend sans rien deviner.

---

## Bloc à copier-coller dans le `CLAUDE.md` du front

```markdown
## Backend API (PetFriendly)

Backend Spring Boot 3.3.4 / Java 21, archi hexagonale — path absolu :
`C:\Users\nicol\IdeaProjects\backend_pet_friendly\`

### Stack front

- **Next.js 15 App Router + TypeScript**
- **Refine** (`@refinedev/core` + `@refinedev/nextjs-router` + data provider REST) — admin headless, CRUD/auth/pagination câblés en conf
- **TanStack Query** (fourni par Refine), UI : Ant Design ou Material (au choix)
- **Client typé généré depuis `openapi.json`** (jamais retaper les types à la main)

### Fichiers de référence côté backend

- **Contrat API** (source de vérité unique) : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\openapi.json`
- **Guide projet backend** : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\CLAUDE.md`
- **Index navigation** : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\docs\MAP.md` (concept → file:line)

### Lancer le backend en local

```bash
export JAVA_HOME="/c/Users/nicol/.jdks/ms-21.0.10"
cd /c/Users/nicol/IdeaProjects/backend_pet_friendly
./mvnw spring-boot:run
```

Disponible ensuite :
- API : `http://localhost:8080/api/v1/`
- Swagger UI : `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON live : `http://localhost:8080/api-docs`

`.env.local` du front : `NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1`

### Comptes seedés (profile dev)

- `admin@petfriendly.fr` / `admin123` — **rôle ADMIN** (accès panel)
- `user@petfriendly.fr` / `user123` — rôle USER (doit être refusé par le panel admin)

### Auth (JWT)

- `POST /auth/login {email,password}` → `{ accessToken, refreshToken, expiresIn, user }`.
  `user` inclut `role` (USER/ADMIN) → **gate le panel sur `role === "ADMIN"`**.
- Bearer dans header `Authorization: Bearer <accessToken>`.
- Access **15 min**, refresh **7 j avec ROTATION** : `POST /auth/refresh {refreshToken}` renvoie une paire neuve, l'ancien refresh est révoqué (un refresh rejoué échoue → anti-replay).
- `POST /auth/logout {refreshToken}` révoque.
- **Stockage tokens** : access en mémoire (state Refine). Refresh en **cookie httpOnly** posé via une Route Handler Next (`app/api/...`), **jamais localStorage**.
- Compte `enabled=false` → login refusé (ban).

### Endpoints (résumé — détails complets dans `openapi.json`)

Base `/api/v1/` :
- **Auth (public)** : POST `/auth/register`, `/login`, `/refresh`, `/logout`
- **Places** : GET public (`/`, `/search`, `/{id}`) · POST auth · PUT/DELETE/`{id}/photos` **owner-only, bypass ADMIN** (403 sinon)
- **Reviews** : GET public (`/places/{id}/reviews`) · POST/PUT/DELETE auth (author-only, sauf delete par auteur)
- **Favorites (auth)** : GET `/users/favorites`, POST/DELETE `/users/favorites/{placeId}`
- **Notifications (auth)** : GET, PATCH `/{id}/read`, DELETE `/{id}`, DELETE `/`
- **Users (auth)** : GET `/users/me` (profil + `role` + stats), PUT `/users/me`, POST `/users/me/avatar`
- **Admin — Users (auth + rôle ADMIN, 403 sinon)** : GET `/admin/users` (paginé), GET `/admin/users/{id}`, PUT `/admin/users/{id}` (nom/rôle/enabled, null=inchangé), DELETE `/admin/users/{id}`

Pagination (list/search/admin) : query `page` (0-based) + `size`. Réponse `PageResponse` = `{ content, page, size, totalElements, totalPages, last }`.

Codes d'erreur : 400 (payload), 401 (JWT absent/invalide), 403 (pas owner / pas ADMIN), 404 (introuvable), 409 (doublon review). Body : `{ timestamp, status, error, path }`.

### Codegen client TS (zéro drift avec le contrat)

```bash
# openapi.json copié/symlinké depuis le backend dans le repo front
npx openapi-typescript ./openapi.json -o src/types/api.d.ts
# alternative avec hooks : orval (orval.config.ts → input openapi.json)
```

À relancer après chaque régénération de `openapi.json` côté back.

### CORS

Dev : backend autorise toutes origines (`*`), rien à faire.
Prod : le back lit `CORS_ALLOWED_ORIGINS` (origines séparées par virgules) → y mettre l'URL du panel déployé.
```

---

## Régénérer `openapi.json` (côté backend)

À faire après tout changement d'endpoint backend, puis recopier côté front pour le codegen :

```bash
cd /c/Users/nicol/IdeaProjects/backend_pet_friendly
./mvnw verify -Pgenerate-openapi -DskipTests
```

---

## Pourquoi ce fichier vit ici (côté backend)

- Versionné en git → toujours la dernière version à jour du contrat côté serveur.
- Le jour où tu crées le front, ouvre ce fichier, copie le bloc, colle dans `front/CLAUDE.md`.
- Si quelque chose change côté backend (URL, compte seedé, refacto auth, nouvel endpoint), mets à jour ce fichier **en même temps** que le code.
