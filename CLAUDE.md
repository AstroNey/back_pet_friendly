# PetFriendly Backend — Guide Claude / Dev

Spec impérative : `src/main/BACKEND_JAVA_SPRING.md` — **toujours relire avant tout dev** (section 15 = ordre d'implémentation).

**Documentation API** : Swagger UI sur `/swagger-ui.html` (OpenAPI JSON sur `/api-docs`). Source de vérité unique pour les endpoints, schémas, codes de retour et exemples.

**Snapshot OpenAPI versionné** : `openapi.json` à la racine, généré par le profil Maven `generate-openapi` :

```bash
./mvnw verify -Pgenerate-openapi -DskipTests
```

À régénérer/commiter à chaque changement d'API. Le diff dans les PRs détecte instantanément toute modification du contrat. Sert aussi de source pour le codegen client Flutter.

**Index `concept → file:line`** : `docs/MAP.md` — premier réflexe pour localiser quoi que ce soit dans le code.

**Setup front (cross-projet)** : `docs/frontend-setup.md` — snippet prêt à coller dans le `CLAUDE.md` d'un futur projet front.

**Décisions structurantes** : `docs/adr/` — Architecture Decision Records (le « pourquoi » des choix archi : hexa, UUID, refresh rotation, MapStruct).

**Sous-agents projet** (`.claude/agents/`) : `endpoint-scaffolder`, `archi-validator`, `map-keeper`, `db-migration-writer`, `swagger-annotator`. Invoquables via `Agent(subagent_type="<nom>")`.

## Stack

- **Spring Boot 3.3.4**, **Java 21** (preview features, virtual threads on)
- **JPA + Flyway**, **PostgreSQL 16 + PostGIS 3.4** (prod), **H2** (dev)
- **MapStruct 1.6.2** + **Lombok 1.18.34** (binding 0.2.0)
- **jjwt 0.12.6** (JWT HS256), **BCrypt strength 12**
- **AWS SDK S3 v2** → MinIO (`FileStoragePort` / `S3FileStorageAdapter`)
- **Firebase Admin 9.4.1** (FCM push)
- **Springdoc 2.6.0** (Swagger `/swagger-ui`, OpenAPI `/api-docs`)
- **Sentry 7.14.0**, **Testcontainers 1.20.1**, **ArchUnit 1.3.0**, **JaCoCo 0.8.12**

## Architecture hexagonale

```
src/main/java/lns/back/backend_pet_friendly/
├── domain/                     # cœur, zéro dep framework
│   ├── model/                  # entités, VO records, enums
│   ├── port/in/                # *UseCase (driving)
│   ├── port/out/               # *Repository, TokenPort, FileStoragePort, NotificationSenderPort
│   └── service/                # *Service (impl des UseCase)
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/             # *JpaEntity (UUID PK, @JdbcTypeCode JSON pour Map/List)
│   │   ├── repository/         # *JpaRepository (Spring Data)
│   │   ├── adapter/            # *RepositoryAdapter (impl ports out)
│   │   └── mapper/             # *Mapper (MapStruct, componentModel="spring")
│   ├── security/               # SecurityConfig, JwtAuthFilter, JwtTokenAdapter
│   ├── storage/                # S3FileStorageAdapter
│   └── notification/           # FirebaseConfig, FcmNotificationAdapter
├── web/
│   ├── controller/             # *Controller (base /api/v1/)
│   ├── dto/request/            # *Request (records)
│   ├── dto/response/           # *Response (records)
│   └── exception/              # GlobalExceptionHandler
└── config/                     # AppConfig, OpenApiConfig, DataSeeder
```

### Règles ArchUnit (`src/test/java/.../ArchitectureTest.java`)

1. `domain` ne dépend PAS de `infrastructure`, `web`, `config`.
2. Les controllers ne s'appellent pas entre eux.
3. `domain.service` autorise uniquement : `domain.*`, `java.*`, slf4j, `@Service`/`@Component`, Spring Security, `Pageable`/`Page`, Lombok.

### Conventions de nommage

| Couche | Suffixe | Exemple |
|---|---|---|
| Use case (port in) | `*UseCase` | `PlaceUseCase` |
| Service domaine | `*Service` | `PlaceService` |
| Port out | `*Port` / `*Repository` | `TokenPort`, `UserRepository` |
| JPA entity | `*JpaEntity` | `PlaceEntity` |
| Spring Data repo | `*JpaRepository` | `PlaceRepository` |
| Adapter persist. | `*RepositoryAdapter` | `PlaceRepositoryAdapter` |
| MapStruct | `*Mapper` | `PlaceMapper` |
| Controller | `*Controller` | `PlaceController` |
| DTO entrée/sortie | `*Request` / `*Response` | `CreatePlaceRequest`, `PlaceResponse` |

## API REST (base `/api/v1/`)

**Vue d'ensemble** des controllers (détails complets dans Swagger UI) :

| Controller | Tag Swagger | Auth |
|---|---|---|
| `AuthController` /auth | Auth | public |
| `PlaceController` /places | Places | GET public, POST/PUT/DELETE auth (owner-only PUT/DELETE) |
| `ReviewController` /api/v1 | Reviews | GET public, POST/DELETE auth (author-only delete) |
| `FavoriteController` /users/favorites | Favorites | auth |
| `NotificationController` /notifications | Notifications | auth |
| `UserController` /users | Users | auth |

`/search` (Places) : query params `q`, `type`, `animals`, `lat`, `lng`, `radius` (km). Avec lat/lng → ST_DWithin natif PostGIS, sinon fallback LIKE.

Ajout d'un endpoint :
1. Annoter avec `@Operation(summary, description)`, `@ApiResponses` pour codes 4xx/404, `@SecurityRequirements` (vide) si public.
2. Annoter le DTO request avec `@Schema(description, example)` sur chaque champ.
3. Vérifier dans Swagger UI que la doc générée est claire.

## Auth (JWT)

- Login/register → access JWT (15 min, claim email) + refresh JWT (7j, **hash stocké en DB** via `RefreshTokenRepository`)
- `/refresh` rotate : nouveau access + nouveau refresh, ancien hash révoqué
- `JwtAuthFilter` parse Bearer header, valide via `TokenPort.isValid()`, set `SecurityContext`
- Routes publiques : `/api/v1/auth/*`, GET `/api/v1/places/*`, Swagger, h2-console
- Stateless, CORS open, CSRF off

## Profiles & config

- **dev** (défaut) : H2 in-memory, Flyway off, `ddl-auto: create-drop`, h2-console `/h2-console`
- **prod** : PostgreSQL via `DATABASE_URL`, Hikari max 10 / min 2, Flyway validate

Migration unique : `src/main/resources/db/migration/V1__init_schema.sql` (extensions UUID-OSSP + PostGIS, tables, `places.location` GEOGRAPHY POINT 4326, GIN index FR sur places.name).

DataSeeder (dev) idempotent : `admin@petfriendly.fr/admin123` (ADMIN), `user@petfriendly.fr/user123` (USER) + 2 places Paris.

## Commandes

```bash
# Prérequis : JAVA_HOME=/c/Users/nicol/.jdks/ms-21.0.10
./mvnw spring-boot:run            # run (profile dev)
./mvnw clean test                 # unit + IT H2 (sans Docker)
./mvnw clean verify               # build + tests + JaCoCo
./mvnw test -Dtest="*Test"        # unit only
./mvnw test -Dtest="*IT"          # IT only (Docker requis pour PostGIS)
```

Sur Windows pour les IT PostGIS Testcontainers : `DOCKER_HOST=tcp://localhost:2375`.

## Tests

- `domain/service/*Test.java` — unit, mocks repos
- `integration/*IT.java` — Spring context + MockMvc, profile dev (H2) sauf si étend `PostgresTestcontainersBase` (PG16+PostGIS3.4 singleton, skip auto si Docker indispo)
- `web/*ContractTest.java` — contrats JSON
- `ArchitectureTest.java` — règles ArchUnit
- Surefire exécute `*Test.java` + `*IT.java` dans la même phase.

JaCoCo exclut entities, DTOs, Application, `@Configuration`. Pas de threshold enforcé.

## Déploiement prod

`docker compose -f docker-compose.prod.yml up -d` avec `.env.prod` (copie de `.env.prod.example`) :

- `db` : postgis/postgis:16-3.4-alpine
- `minio` + `minio-init` (création bucket)
- `app` : multi-stage Temurin JDK21→JRE21 Alpine, user `petfriendly`, healthcheck `/actuator/health`
- `caddy` : reverse-proxy `/api/*`, `/actuator/health`, `/swagger-ui*`, `/api-docs*` → app:8080, Let's Encrypt via `${DOMAIN}` + `${ACME_EMAIL}`

Env vars : `POSTGRES_*`, `DATABASE_URL`, `DB_USER/PASSWORD`, `MINIO_ROOT_*`, `S3_BUCKET/ENDPOINT/ACCESS_KEY/SECRET_KEY`, `JWT_SECRET` (256+ bits base64), `JWT_EXPIRATION_MINUTES`, `JWT_REFRESH_DAYS`, `DOMAIN`, `ACME_EMAIL`, `SENTRY_DSN`, `SENTRY_TRACES_RATE`, `APP_VERSION`.

## Pour Claude — règles de collaboration

- **Index `concept → file:line`** : `docs/MAP.md` — consulter en priorité avant de gréper pour retrouver "où est X". Mettre à jour quand un concept change de place.
- Lire la spec (`src/main/BACKEND_JAVA_SPRING.md`) avant tout dev impactant l'archi.
- Respecter les suffixes ci-dessus, jamais d'annotation Spring dans `domain/model/` ou `domain/port/`.
- Tout nouvel ID = `UUID`.
- Mapping domain ↔ JPA via MapStruct, pas à la main.
- Avant de proposer une commande Maven, rappeler `JAVA_HOME` si absent.
- Tests : un service touché → un `*Test` unitaire ; un controller touché → un `*IT` MockMvc.
