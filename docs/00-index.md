# Documentation — PetFriendly Backend

API REST Java 21 / Spring Boot 3.4.4 en architecture hexagonale.

## Modules

| # | Fichier | Contenu |
|---|---------|---------|
| 1 | [Architecture hexagonale](01-architecture-hexagonale.md) | Principe général, structure des packages, flux bout en bout |
| 2 | [Domaine — Modèles](02-domaine-modeles.md) | Place, User, Review, Notification, Coordinates, Enums |
| 3 | [Domaine — Ports](03-domaine-ports.md) | Ports IN (Use Cases), Ports OUT (Repositories, TokenPort…) |
| 4 | [Domaine — Services](04-domaine-services.md) | Implémentation des Use Cases, logique métier |
| 5 | [Infra — Persistence](05-infra-persistence.md) | JPA Entities, Spring Data, MapStruct, Adapters |
| 6 | [Infra — Sécurité](06-infra-security.md) | JWT, Spring Security, BCrypt, JwtAuthFilter |
| 7 | [Web Layer](07-web-layer.md) | Contrôleurs REST, DTOs records, gestion d'erreurs |
| 8 | [Infra — Stockage & Notifs](08-infra-storage-notifications.md) | S3 (mock), FCM push (mock) |

## Stack technique

- **Java 21** — Virtual threads, records, sealed classes
- **Spring Boot 3.4.4** — Web, Security, Data JPA, Validation
- **H2** (dev) / **PostgreSQL + PostGIS** (prod)
- **jjwt 0.12.6** — JWT (access 15min, refresh 7j)
- **MapStruct** — Mapping compile-time
- **Lombok** — Boilerplate Java
- **MinIO/S3** — Stockage images (TODO)
- **Firebase FCM** — Push notifications (TODO)

## Démarrage

```bash
export JAVA_HOME="/c/Users/nicol/.jdks/ms-21.0.10"
./mvnw spring-boot:run
```

Comptes de test :
- `admin@petfriendly.com` / `admin123`
- `user@petfriendly.com` / `user123`