# Infrastructure — Persistence

Chemin : `infrastructure/persistence/`

Traduit entre le domaine (objets purs) et la base de données (JPA). 4 sous-couches :

```
domain/model/Place  ←→  PlaceMapper  ←→  PlaceJpaEntity  ←→  PlaceJpaRepository (SQL)
                             ↑
                    PlaceRepositoryAdapter
                    (implémente domain/port/out/PlaceRepository)
```

## 1. Entités JPA — `entity/`

Suffixe `*JpaEntity`. UUID PK natifs, `@JdbcTypeCode(SqlTypes.JSON)` pour les Map/List, location géographique en colonne PostGIS.

```java
@Entity @Table(name = "places")
@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PlaceJpaEntity {

    @Id @Column(columnDefinition = "uuid")
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlaceType type;

    private Double latitude;
    private Double longitude;
    // location GEOGRAPHY(Point, 4326) en SQL (cf. V1__init_schema.sql), gérée
    // par trigger / colonne calculée pour ST_DWithin

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AnimalType> animals;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> galleryUrls;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> hours;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}
```

**Pourquoi :**
- `columnDefinition = "uuid"` → type natif PostgreSQL (avec H2 en dev, fallback transparent).
- `@JdbcTypeCode(SqlTypes.JSON)` : sérialisation JSON native (pas de table de jointure pour les List/Map). Compatible H2 et PostgreSQL.
- Enums stockés en `STRING` — stable si on réordonne l'enum.
- Timestamps gérés par Hibernate.

`FavoriteJpaEntity` utilise une clé composite `@EmbeddedId` (record `FavoriteId(userId, placeId)`).

## 2. Spring Data Repositories — `repository/`

Suffixe `*JpaRepository`. Étendent `JpaRepository<Entity, UUID>`.

`PlaceJpaRepository` expose deux modes de recherche :
- `search(type, text, pageable)` — JPQL portable (LIKE sur le nom)
- `searchNearby(lat, lng, radiusMeters, ...)` — `@Query(nativeQuery = true)` avec `ST_DWithin` + `ST_Distance` sur PostGIS, ordonné par distance

Le bon mode est choisi côté adapter selon que `query.location()` est null ou pas.

## 3. Mappers MapStruct — `mapper/`

Génération de code à la **compilation** (pas de réflexion runtime).

```java
@Mapper(componentModel = "spring")
public interface PlaceMapper {

    @Mapping(target = "latitude",  source = "coordinates.latitude")
    @Mapping(target = "longitude", source = "coordinates.longitude")
    @Mapping(target = "reviews", ignore = true)
    PlaceJpaEntity toEntity(Place place);

    @Mapping(target = "coordinates",
             expression = "java(new Coordinates(entity.getLatitude(), entity.getLongitude()))")
    Place toDomain(PlaceJpaEntity entity);
}
```

- `Place` a un `Coordinates` record, `PlaceJpaEntity` a deux colonnes — le mapper reconstruit.
- `componentModel = "spring"` → bean `@Component` injectable.
- Code généré dans `target/generated-sources/`.

Annotation processing dans pom.xml : MapStruct + Lombok ordonnés via `lombok-mapstruct-binding 0.2.0`.

## 4. Adapters — `adapter/`

Suffixe `*RepositoryAdapter`. Implémentent les ports OUT du domaine.

```java
@Component @RequiredArgsConstructor
public class PlaceRepositoryAdapter implements PlaceRepository {

    private final PlaceJpaRepository jpaRepository;
    private final PlaceMapper mapper;

    @Override public Optional<Place> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override public Place save(Place place) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(place)));
    }

    @Override public Page<Place> search(SearchUseCase.SearchQuery query, Pageable pageable) {
        return query.location() != null
            ? jpaRepository.searchNearby(/* ... */).map(mapper::toDomain)
            : jpaRepository.search(query.type(), query.q(), pageable).map(mapper::toDomain);
    }
}
```

Adapters intentionnellement fins : mapping + délégation, **zéro logique métier**.

## Migrations — `src/main/resources/db/migration/`

Une seule migration : `V1__init_schema.sql`.
- Extensions `uuid-ossp` et `postgis`
- Tables `users`, `places`, `reviews`, `favorites`, `refresh_tokens`, `notifications`
- `places.location GEOGRAPHY(Point, 4326)` pour ST_DWithin
- Index GIN full-text français sur `places.name`

Profile **dev** : Flyway off, Hibernate `ddl-auto: create-drop` (H2 mémoire).
Profile **prod** : Flyway on, Hibernate `validate`.

## À retenir

- Modèles domaine et JPA entities sont **deux représentations distinctes** — ne jamais fusionner.
- MapStruct résout les différences de structure (`Coordinates` ↔ lat/lng).
- `@JdbcTypeCode(SqlTypes.JSON)` fonctionne en H2 et PostgreSQL sans changer de code.
- PostGIS (`ST_DWithin`) actif uniquement en prod ; en dev H2, le fallback LIKE est utilisé automatiquement.
