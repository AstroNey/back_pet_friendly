# Documentation — PetFriendly Backend

API REST Java 21 / Spring Boot 3.3.4 en architecture hexagonale.

## Documentation API → Swagger

- **Swagger UI** : http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** : http://localhost:8080/api-docs
- **Snapshot versionné** : `openapi.json` à la racine du repo (régénéré via `./mvnw verify -Pgenerate-openapi -DskipTests`)

C'est la **source de vérité unique** pour endpoints, payloads, codes de retour, exemples.

## Index "concept → file:line"

[`MAP.md`](MAP.md) — table de tous les concepts non-triviaux du projet (auth flow, règles métier, requêtes PostGIS, fallbacks S3/FCM, etc.) avec leur fichier:ligne précis. À consulter en priorité pour économiser des `Grep`.

## Documentation interne (architecture & infra)

Couvre uniquement ce qui n'est pas dans Swagger : concepts, patterns, choix d'implémentation.

| # | Fichier | Contenu |
|---|---------|---------|
| 1 | [Architecture hexagonale](01-architecture-hexagonale.md) | Principe, structure des packages, flux bout en bout, règles ArchUnit |
| 5 | [Infra — Persistence](05-infra-persistence.md) | JPA entities, MapStruct, RepositoryAdapter, PostGIS, migrations Flyway |
| 6 | [Infra — Sécurité](06-infra-security.md) | JWT access/refresh avec rotation, BCrypt, JwtAuthFilter |
| 8 | [Infra — Stockage & Notifs](08-infra-storage-notifications.md) | S3/MinIO (AWS SDK v2), FCM Firebase Admin, dégradation gracieuse |

> Modules 02 (modèles), 03 (ports), 04 (services), 07 (web layer) supprimés : redondants avec le code source et avec Swagger.

## Doc Frontend

- [`flutter-frontend.md`](flutter-frontend.md) — contrat API côté client Flutter.
- [`frontend-setup.md`](frontend-setup.md) — snippet prêt à copier dans le `CLAUDE.md` d'un futur projet front (paths absolus, comptes seedés, endpoints).

## Stack technique

- **Java 21** — virtual threads, records, preview features
- **Spring Boot 3.3.4** — Web, Security, Data JPA, Validation, Actuator
- **H2** (dev) / **PostgreSQL 16 + PostGIS 3.4** (prod)
- **jjwt 0.12.6** — JWT (access 15 min, refresh 7 j avec rotation)
- **MapStruct 1.6.2** — mapping compile-time
- **Lombok 1.18.34** + binding 0.2.0
- **AWS SDK S3 v2** — stockage MinIO/S3
- **Firebase Admin 9.4.1** — push FCM
- **Springdoc 2.6.0** — OpenAPI / Swagger UI
- **Sentry 7.14.0** — error tracking
- **Testcontainers 1.20.1** + **ArchUnit 1.3.0** + **JaCoCo 0.8.12**

## Démarrage

```bash
export JAVA_HOME="/c/Users/nicol/.jdks/ms-21.0.10"
./mvnw spring-boot:run            # profile dev (H2 mémoire)
```

Comptes seedés (profile dev, idempotent) :
- `admin@petfriendly.fr` / `admin123` (ADMIN)
- `user@petfriendly.fr` / `user123` (USER)

## Spec impérative

`src/main/BACKEND_JAVA_SPRING.md` — à relire avant tout dev impactant l'archi (section 15 = ordre d'implémentation).
