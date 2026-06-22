# ADR 0001 — Architecture hexagonale (Ports & Adapters)

- **Date** : 2026-04-20
- **Status** : Accepted

## Context

Démarrage projet. Premier scaffold initial fait en archi en couches classique (Controller → Service → Repository, tout dans le même package). La spec `BACKEND_JAVA_SPRING.md` impose hexagonal. Refacto avant que le code grossisse.

## Decision

Adopter l'archi hexagonale stricte :
- `domain/` — modèles, ports IN (use cases), ports OUT (repositories), services. Zéro dépendance framework sauf `@Service`/`@Component` et `Pageable`/`Page` Spring Data (pragma).
- `infrastructure/` — adapters JPA, sécurité, S3, FCM. Dépend du domain, jamais l'inverse.
- `web/` — controllers, DTOs. Dépend des ports IN du domain, jamais d'`infrastructure/` directement.

Règles enforcées par ArchUnit (`src/test/java/.../ArchitectureTest.java`).

## Consequences

**Positif**
- Tests unitaires triviaux (`new PlaceService(mockRepo)`, pas de `@SpringBootTest`).
- Stack swappable (Spring → Quarkus impacte infra/, pas domain/).
- Lisibilité : `domain/` se lit comme du Java pur.
- Pas d'Anemic Domain : la logique vit dans les objets (`Place.addReview` recalcule le rating).

**Négatif**
- Plus de fichiers (entity domaine + entity JPA + mapper + adapter) pour chaque concept persisté.
- Mappings MapStruct à maintenir.
- Concession : `Page`/`Pageable` Spring Data tolérés dans les ports (sinon il faudrait réécrire un Page<T> maison).
