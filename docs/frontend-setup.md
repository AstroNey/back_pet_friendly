# Setup côté front — pour relier un projet front à ce backend

Le jour où tu crées un projet front (Flutter / web / autre), copie le bloc ci-dessous dans son **`CLAUDE.md`** racine. Toute session Claude côté front aura alors le contexte minimal pour appeler ce backend sans deviner quoi que ce soit.

---

## Bloc à copier-coller dans le `CLAUDE.md` du front

```markdown
## Backend API

Backend Spring Boot 3.3.4 / Java 21 — path absolu :
`C:\Users\nicol\IdeaProjects\backend_pet_friendly\`

### Fichiers de référence côté backend

- **Contrat API** (source de vérité unique) : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\openapi.json`
- **Guide projet backend** : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\CLAUDE.md`
- **Spec impérative** : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\src\main\BACKEND_JAVA_SPRING.md`
- **Doc front-spécifique** : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\docs\flutter-frontend.md`
- **Doc archi backend** : `C:\Users\nicol\IdeaProjects\backend_pet_friendly\docs\01-architecture-hexagonale.md` (et 05, 06, 08)

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

### Comptes seedés (profile dev)

- `admin@petfriendly.fr` / `admin123` (ADMIN)
- `user@petfriendly.fr` / `user123` (USER, pet "Labrador")

### Auth

JWT Bearer dans `Authorization`. Access 15 min, refresh 7 jours **avec rotation** (l'ancien refresh est révoqué à chaque appel `/auth/refresh`).

### Endpoints (résumé — détails dans `openapi.json`)

Base `/api/v1/` :
- **Auth (public)** : POST `/auth/register`, `/login`, `/refresh`, `/logout`
- **Places** : GET public (`/`, `/search`, `/{id}`), POST/PUT/DELETE auth (owner-only PUT/DELETE)
- **Reviews** : GET public (`/places/{id}/reviews`), POST/DELETE auth (author-only delete)
- **Favorites (auth)** : GET `/users/favorites`, POST/DELETE `/users/favorites/{placeId}`
- **Notifications (auth)** : GET, PATCH `/{id}/read`, DELETE `/{id}`, DELETE `/`
- **Users (auth)** : GET/PUT `/users/me`

### Régénérer `openapi.json`

À faire après tout changement d'endpoint backend :

```bash
cd /c/Users/nicol/IdeaProjects/backend_pet_friendly
./mvnw verify -Pgenerate-openapi -DskipTests
```

### Codegen client

`openapi-generator` peut produire un client Dart/Flutter (entre autres) depuis `openapi.json` — voie recommandée pour aligner les types automatiquement plutôt que les retaper.
```

---

## Pourquoi ce fichier vit ici (côté backend)

- Le front n'existe pas encore : le snippet est prêt mais pas encore consommé.
- Versionné en git → tu retrouves toujours la dernière version à jour.
- Le jour où tu crées le front, tu ouvres ce fichier, copie le bloc, colle dans `front/CLAUDE.md`. Done.

Si quelque chose change côté backend (nouvelle URL, nouveau compte seedé, refacto de l'auth…), mets à jour ce fichier en même temps que le code.
