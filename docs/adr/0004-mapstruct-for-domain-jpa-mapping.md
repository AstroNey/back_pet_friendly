# ADR 0004 — MapStruct pour mapping domain ↔ JPA entity ↔ DTO

- **Date** : 2026-04-21
- **Status** : Accepted

## Context

L'archi hexagonale impose 3 représentations distinctes :
- `domain/model/Place` — POJO pur Java
- `infrastructure/persistence/entity/PlaceJpaEntity` — annotations JPA
- `web/dto/response/PlaceResponse` — record exposé par REST

Donc 3 conversions à coder pour chaque entité. Options :
1. **À la main** dans chaque adapter — verbeux, sujet à bugs (oublier un champ).
2. **ModelMapper** runtime réflexion — lent, magic, erreurs au runtime.
3. **MapStruct** compile-time codegen (choix retenu).

## Decision

MapStruct 1.6.2 avec `@Mapper(componentModel = "spring")`. Annotation processing dans `pom.xml` ordonné : MapStruct + Lombok + `lombok-mapstruct-binding` 0.2.0. Code généré dans `target/generated-sources/`.

## Consequences

**Positif**
- Code de mapping compile-time : zéro réflexion runtime, performance native.
- Erreurs détectées à la **compilation** (champ manquant, type incompatible).
- Lisible : on voit le code généré dans `target/generated-sources/`.
- Mappings non-triviaux gérés par `@Mapping` (ex: `Coordinates` ↔ `latitude/longitude`) ou `expression = "java(...)"`.
- Bean Spring `@Component` injectable.

**Négatif**
- Setup `annotationProcessorPaths` à maintenir (ordre Lombok-MapStruct critique → `lombok-mapstruct-binding`).
- IDE doit déclencher l'annotation processing pour résoudre les imports (`*MapperImpl`).
- Stack trace d'erreur peut être indirect (passe par le code généré).

**Convention**
- Suffixe `*Mapper` dans `infrastructure/persistence/mapper/`.
- Côté DTO : on garde des méthodes statiques `*Response.from(domain)` (pas MapStruct, mappings simples).
