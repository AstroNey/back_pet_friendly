# MAP — Concept → fichier:ligne

Index alphabétique-fonctionnel des concepts non-triviaux du projet. Évite les `Grep` répétitifs pour retrouver "où est X". Tous les paths sont relatifs à la racine du repo.

> **Maintenance** : à mettre à jour quand un concept listé change de location ou qu'un nouveau concept non-trivial apparaît. Ne pas indexer les CRUD basiques (déjà couverts par les conventions de nommage).

---

## Auth & sécurité

| Concept | Fichier:ligne | Détail |
|---|---|---|
| Issue tokens (access + refresh) | `domain/service/AuthService.java:89` | `issueTokens` — génère JWT access (15 min, claim email) + refresh (7 j) + persiste hash |
| Refresh token rotation | `domain/service/AuthService.java:57` | Valide JWT, lookup hash, révoque l'ancien (`revokedAt`), émet nouvelle paire |
| Logout (révocation) | `domain/service/AuthService.java:78` | Marque `revokedAt` sur le hash en DB |
| SHA-256 du refresh | `domain/service/TokenHasher.java:11` | Hash hex stocké en DB, jamais le token en clair |
| Generate access token | `infrastructure/security/JwtTokenAdapter.java:20` | jjwt, subject=userId, claim email, exp 15 min |
| Generate refresh token | `infrastructure/security/JwtTokenAdapter.java:25` | jjwt, claim type=refresh, exp 7 j |
| JWT validation | `infrastructure/security/JwtTokenAdapter.java:33` | `isValid` — try/catch parseSignedClaims |
| Extract userId from JWT | `infrastructure/security/JwtTokenAdapter.java:30` | `extractUserId` — UUID depuis subject |
| BCrypt configuration | `infrastructure/security/SecurityConfig.java:25` | `BCryptPasswordEncoder(12)` — strength 12, ~250 ms/hash |
| RBAC — rôle → authority | `infrastructure/security/UserDetailsServiceAdapter.java:26` | `ROLE_<role>` depuis `User.role` (USER/ADMIN). Propage aussi `enabled` (compte banni → login refusé) |
| Method security + route admin | `infrastructure/security/SecurityConfig.java:22,40` | `@EnableMethodSecurity` + `/api/v1/admin/**` → `hasRole("ADMIN")`. Endpoints admin aussi annotés `@PreAuthorize` |
| Ban (login bloqué si désactivé) | `domain/service/AuthService.java:54` | `login` refuse si `!user.isEnabled()` |
| SecurityFilterChain (routes) | `infrastructure/security/SecurityConfig.java:29` | Routes publiques : `/auth/**`, GET `/places/**`, Swagger, h2-console |
| JWT filter doFilterInternal | `infrastructure/security/JwtAuthFilter.java:24` | Parse Bearer, valide, peuple SecurityContext |
| Load user by id | `infrastructure/security/UserDetailsServiceAdapter.java:21` | Bridge UserRepository → Spring Security `UserDetails` |
| RefreshToken.isActive | `domain/model/RefreshToken.java:30` | `!isRevoked() && !isExpired()` |

## Domaine — règles métier

| Concept | Fichier:ligne | Détail |
|---|---|---|
| 1 review par user et par place | `domain/service/ReviewService.java` (`create`) | Check `existsByPlaceIdAndAuthorId` → `DuplicateReviewException` (→ **409**) |
| Modération avis (statut) | `domain/model/ReviewStatus.java` + `domain/model/Review.java` | Enum PENDING/APPROVED/REJECTED + `status`/`moderatedAt`/`moderatedBy`. Nouvel avis = PENDING |
| Lecture publique APPROVED only | `domain/service/ReviewService.java` (`getByPlace`) | `findApprovedByPlaceId` — seuls les APPROVED listés. Idem recalc note (`countApprovedByPlaceId`/`averageApprovedRatingByPlaceId`) |
| Mes avis (tous statuts) | `domain/service/ReviewService.java` (`getByAuthor`) + `web/controller/ReviewController.java` (`getMyReviews`) | `GET /api/v1/users/me/reviews` — l'auteur voit ses PENDING/REJECTED |
| Modérer un avis (ADMIN) | `domain/service/ReviewService.java` (`moderate`) + `web/controller/AdminReviewController.java` | `GET/PATCH /api/v1/admin/reviews` — approuve/rejette, set moderatedAt/By, recalc place. `placeName` peuplé via `enrichPlaceName` |
| Re-modération sur édition | `domain/service/ReviewService.java` (`update`) | PUT par l'auteur → repasse `status=PENDING`, reset moderatedAt/By |
| Author-only delete review | `domain/service/ReviewService.java` | `delete`/`update` lancent `AccessDeniedException` si `authorId != requesterId` |
| Édition review (auteur-only) | `domain/service/ReviewService.java` (`update`) | `PUT /api/v1/reviews/{id}` — remplace rating+text, recalcule place |
| Recalcul rating/reviewCount place | `domain/service/ReviewService.java` (`recalcPlaceRating`) | Agrégat `count`+`AVG` depuis tous les avis (la liste en mémoire du Place est vide — mapper ignore). Appelé sur create/update/delete |
| Doublon review → 409 | `web/exception/GlobalExceptionHandler.java` + `domain/exception/DuplicateReviewException.java` | Exception domaine (archi : domain.service ne peut throw web.*) |
| Owner-only place (update/delete/upload) | `domain/service/PlaceService.java` (`requireOwnerOrAdmin`) | `AccessDeniedException` (→ **403**) si requester ≠ `ownerId`, sauf ADMIN (bypass). Owner null → seul ADMIN. Câblé sur update/delete/uploadImage |
| Suppression lieux en lot (ADMIN) | `domain/service/PlaceService.java` (`deleteAll`) + `web/controller/AdminPlaceController.java` | `DELETE /api/v1/admin/places` body `{ids:[...]}` → supprime le lot, ignore ids inexistants, renvoie `{requested,deleted}`. `@PreAuthorize("hasRole('ADMIN')")` |
| Import lieux en masse (ADMIN) | `domain/service/PlaceImportService.java` + `web/controller/AdminPlaceController.java` + `infrastructure/async/PlaceImportExecutorAdapter.java` | `POST /api/v1/admin/places/import` multipart JSON (jusqu'à 10k lieux) → 202 + `{jobId}`. `GET /api/v1/admin/places/import/{jobId}` pour suivre. Async via `ImportProcessorPort` (`@Async` en infra). Job persisté en DB (`import_jobs`). |
| isAdmin depuis le JWT | `web/controller/PlaceController.java` (`isAdmin`) | Lit l'authority `ROLE_ADMIN` du `UserDetails`, passé aux use cases place |
| Place : recalcul rating | `domain/model/Place.java:49` | `addReview` met à jour rating + reviewCount |
| Coordinates validation | `domain/model/Coordinates.java:5` | Compact constructor, lat ∈ [-90,90], lng ∈ [-180,180] |
| Distance Haversine | `domain/model/Coordinates.java:11` | `distanceTo(other)` en km |
| Email unicité | `domain/service/AuthService.java:32` | Check `existsByEmail` au register |
| Toggle favori | `domain/service/FavoriteService.java:26` | Add si absent, remove sinon |
| User stats | `domain/service/UserService.java:42` | Agrège reviewsWritten + favoritesCount + placesAdded |
| Admin — gestion users | `domain/service/UserService.java` (`listAll`/`adminUpdate`/`delete`) | Liste paginée, update nom/rôle/enabled (null = inchangé), suppression définitive |
| Resource not found → 404 | `domain/exception/ResourceNotFoundException.java` + `GlobalExceptionHandler` | Exception **domaine** (vs `web.ResourceNotFoundException` inutilisée). `UserService.getById` la lève |
| Notification create | `domain/service/NotificationService.java:30` | Persiste + pousse via `NotificationSenderPort` |

## Persistence

| Concept | Fichier:ligne | Détail |
|---|---|---|
| Recherche JPQL (LIKE) | `infrastructure/persistence/repository/PlaceJpaRepository.java:13` | Fallback sans géoloc — name + address + type + filtre animals (OR via `EXISTS … p.animals a IN :animals`) |
| Recherche PostGIS native | `infrastructure/persistence/repository/PlaceJpaRepository.java:25` | `ST_DWithin` + `ST_Distance` ordré par distance + filtre animals (`EXISTS … place_animals`) |
| Filtre animals (OR) — câblage | `infrastructure/persistence/adapter/PlaceRepositoryAdapter.java:32` | Lit `q.animals()`, `filterAnimals` + liste fallback non-vide (évite empty-IN) |
| Animaux acceptés (stockage) | `infrastructure/persistence/entity/PlaceJpaEntity.java:27` | `@ElementCollection` → table `place_animals(place_id, animal)`, `@Enumerated(STRING)`. **Pas** colonne JSON |
| Favoris d'un user | `infrastructure/persistence/repository/PlaceJpaRepository.java:18` | JOIN `FavoriteJpaEntity` sur `(userId, placeId)` |
| Migration init | `src/main/resources/db/migration/V1__init_schema.sql` | Extensions UUID-OSSP + PostGIS, toutes tables, GIN FR sur `places.name` |
| Migration modération avis | `src/main/resources/db/migration/V2__review_moderation.sql` | Colonnes `status`/`moderated_at`/`moderated_by` + index. Grandfather : avis existants → APPROVED |

## Mapping & ports

| Concept | Fichier:ligne | Détail |
|---|---|---|
| TokenPort interface | `domain/port/out/TokenPort.java:6` | 4 méthodes — generateAccess(uid, email), generateRefresh(uid), extractUserId, isValid |
| FileStoragePort interface | `domain/port/out/FileStoragePort.java:4` | 2 méthodes — upload, delete |
| NotificationSenderPort interface | `domain/port/out/NotificationSenderPort.java:12` | `sendPush(uid, title, body, data)` |
| Mapping Coordinates ↔ lat/lng | `infrastructure/persistence/mapper/PlaceMapper.java` | `@Mapping` source/target sur `coordinates.latitude` etc. |

## Infra technique

| Concept | Fichier:ligne | Détail |
|---|---|---|
| S3 init avec fallback | `infrastructure/storage/S3FileStorageAdapter.java:47` | `@PostConstruct`, dégradation graceful si MinIO indispo. Conditionnel via `petfriendly.storage.type=s3` (défaut) |
| S3 upload + sanitize | `infrastructure/storage/S3FileStorageAdapter.java:63` | UUID prefix + regex sanitization |
| LocalFileStorageAdapter init | `infrastructure/storage/LocalFileStorageAdapter.java:32` | Crée le root-dir, mode local activé via `petfriendly.storage.type=local` |
| Local files served on /files/** | `infrastructure/storage/LocalFileStorageWebConfig.java:25` | Mappe `/files/**` sur le disque |
| FCM no-op fallback | `infrastructure/notification/FcmNotificationAdapter.java:39` | `firebaseMessaging == null` → log debug only |
| Firebase config | `infrastructure/notification/FirebaseConfig.java` | Lit `petfriendly.firebase.service-account-file`, ne crée pas le bean si vide |

## Web layer

| Concept | Fichier:ligne | Détail |
|---|---|---|
| OpenAPI bean (Swagger) | `config/OpenApiConfig.java:22` | Bearer JWT scheme, servers (local + prod), tags |
| Auth controller | `web/controller/AuthController.java:24-46` | register/login/refresh/logout — `@SecurityRequirements` (public) |
| Place controller | `web/controller/PlaceController.java:32-104` | list, search (PostGIS), getById, create/update/delete |
| Place photo upload (multipart) | `web/controller/PlaceController.java:117` | `POST /api/v1/places/{id}/photos` — multipart, JWT, delegates to `placeUseCase.uploadImage` |
| Review controller | `web/controller/ReviewController.java:27-54` | byPlace, create, delete |
| Favorite controller | `web/controller/FavoriteController.java:25-42` | get, toggle, remove |
| Notification controller | `web/controller/NotificationController.java:23-55` | list, markRead, delete, clearAll |
| User controller | `web/controller/UserController.java:24-47` | me (profil + stats), update |
| User avatar upload (multipart) | `web/controller/UserController.java:60` | `POST /api/v1/users/me/avatar` — multipart, JWT, delegates to `userUseCase.uploadAvatar` |
| Admin user controller | `web/controller/AdminUserController.java` | `/api/v1/admin/users` — list/get/update/delete, `@PreAuthorize("hasRole('ADMIN')")` au niveau classe |

## Bootstrapping & config

| Concept | Fichier:ligne | Détail |
|---|---|---|
| Seeded users + places | `config/DataSeeder.java:21` | Idempotent : skip si admin existe ; admin/user + 2 places Paris |
| Profile dev (H2) | `src/main/resources/application-dev.yml` | H2 mémoire, Flyway off, ddl-auto create-drop, h2-console |
| Profile prod (PostgreSQL) | `src/main/resources/application-prod.yml` | PG via DATABASE_URL, Hikari 10/2, Flyway validate |
| Application base config | `src/main/resources/application.yml` | Virtual threads on, Springdoc paths, JWT/storage env vars |

## Tests

| Concept | Fichier:ligne | Détail |
|---|---|---|
| ArchUnit — domain isolation | `src/test/java/.../ArchitectureTest.java:11` | `domain` ne dépend pas de `infrastructure`, `web`, `config` |
| ArchUnit — controllers indep | `src/test/java/.../ArchitectureTest.java:17` | Controllers ne s'appellent pas entre eux |
| ArchUnit — domain.service deps | `src/test/java/.../ArchitectureTest.java:22` | Whitelist : domain, java, slf4j, @Service/@Component, Spring Security, Pageable, Lombok |
| Testcontainers PG+PostGIS base | `src/test/java/.../integration/PostgresTestcontainersBase.java` | Singleton container, skip si Docker indispo |

## Build

| Concept | Fichier:ligne | Détail |
|---|---|---|
| OpenAPI snapshot generation | `pom.xml` (profile `generate-openapi`) | Démarre app sur 8095 → scrape `/api-docs` → écrit `openapi.json` racine |
| Annotation processing | `pom.xml` (`annotationProcessorPaths`) | MapStruct + Lombok + lombok-mapstruct-binding 0.2.0 |
| JaCoCo exclusions | `pom.xml` (jacoco plugin config) | Exclut entities, DTOs, Application, @Configuration |

---

## Comment l'utiliser efficacement (Claude side)

1. Pour une question "où est X", chercher d'abord dans cette table avant de gréper.
2. Le file:line peut bouger d'une session à l'autre (refacto, ajout de méthode) — vérifier avec un `Read` ciblé sur la zone si la ligne semble fausse.
3. Si un concept manque ici alors qu'il devrait y être, l'ajouter immédiatement après l'avoir trouvé.
