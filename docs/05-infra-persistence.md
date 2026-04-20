# Module : Infrastructure — Persistence

Chemin : `infrastructure/persistence/`

Ce module traduit entre le monde domaine (objets purs) et le monde base de données (JPA). Il est organisé en 4 sous-couches.

```
domain/model/Place  ←→  PlaceMapper  ←→  PlaceJpaEntity  ←→  PlaceJpaRepository (SQL)
                             ↑
                    PlaceRepositoryAdapter
                    (implémente PlaceRepository)
```

---

## 1. Entités JPA — `entity/`

Les entités JPA sont la représentation base de données des modèles domaine. Elles portent toutes les annotations de mapping (`@Entity`, `@Column`, etc.) que les modèles domaine n'ont pas.

### Exemple : `PlaceJpaEntity`

```java
@Entity @Table(name = "places")
@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PlaceJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private PlaceType type;

    private Double latitude;
    private Double longitude;
    // Note: le domaine a Coordinates (record), la BDD a deux colonnes séparées

    @JdbcTypeCode(SqlTypes.JSON)
    private List<AnimalType> acceptedAnimals;   // stocké en JSON natif PostgreSQL

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> openingHours;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

**Points importants :**
- Les UUID sont stockés nativement (`columnDefinition = "uuid"` pour PostgreSQL).
- `@JdbcTypeCode(SqlTypes.JSON)` : les `List<>` et `Map<>` sont sérialisés en colonne JSON PostgreSQL. Aucune table de jointure pour ces données.
- Les enums sont stockés en `STRING` (`"DOG"` pas `0`) — stable si on réordonne l'enum.
- Les timestamps sont gérés automatiquement par Hibernate.

### `FavoriteJpaEntity` — Clé composite

```java
@Entity @Table(name = "favorites")
public class FavoriteJpaEntity {

    @EmbeddedId
    private FavoriteId id;   // userId + placeId

    private LocalDateTime createdAt;

    @Embeddable
    public record FavoriteId(UUID userId, UUID placeId) implements Serializable {}
}
```

Pas de surrogate key ici : la combinaison `(userId, placeId)` est naturellement unique. `@EmbeddedId` avec un `record` est la façon idiomatique en JPA 3.x.

---

## 2. Spring Data Repositories — `repository/`

Des interfaces qui étendent `JpaRepository<Entity, ID>`. Spring génère l'implémentation SQL au démarrage.

```java
public interface PlaceJpaRepository extends JpaRepository<PlaceJpaEntity, UUID> {

    @Query("""
        SELECT p FROM PlaceJpaEntity p
        WHERE (:type IS NULL OR p.type = :type)
        AND (:text IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :text, '%')))
        """)
    Page<PlaceJpaEntity> search(
        @Param("type") PlaceType type,
        @Param("text") String text,
        Pageable pageable
    );
}
```

- La méthode `search` utilise JPQL (pas SQL natif) pour rester portable.
- La pagination est gérée automatiquement : Spring traduit `Pageable` en `LIMIT/OFFSET`.
- Pour la géolocalisation réelle (PostGIS), on utilisera `ST_DWithin` dans une `@NativeQuery`.

---

## 3. Mappers MapStruct — `mapper/`

MapStruct génère du code de mapping à la **compilation** (pas de réflexion au runtime). C'est plus rapide et plus sûr que des outils comme ModelMapper.

```java
@Mapper(componentModel = "spring")
public interface PlaceMapper {

    @Mapping(target = "latitude", source = "coordinates.latitude")
    @Mapping(target = "longitude", source = "coordinates.longitude")
    @Mapping(target = "reviews", ignore = true)
    PlaceJpaEntity toEntity(Place place);

    @Mapping(target = "coordinates",
             expression = "java(new Coordinates(entity.getLatitude(), entity.getLongitude()))")
    Place toDomain(PlaceJpaEntity entity);
}
```

**Pourquoi :**
- `Place` a un `Coordinates` (record imbriqué), `PlaceJpaEntity` a `latitude` + `longitude` (colonnes séparées). Le mapper fait la conversion.
- `reviews` est ignoré dans `toEntity` car les avis sont une relation gérée séparément.
- `componentModel = "spring"` → MapStruct génère une classe `@Component` injectable.

Le code généré (`PlaceMapperImpl.java`) est visible dans `target/generated-sources/`.

---

## 4. Adapters — `adapter/`

Les adapters **implémentent les interfaces** de port OUT définies dans le domaine. Ils font le lien entre le domaine et Spring Data.

```java
@Component @RequiredArgsConstructor
public class PlaceRepositoryAdapter implements PlaceRepository {

    private final PlaceJpaRepository jpaRepository;
    private final PlaceMapper mapper;

    @Override
    public Optional<Place> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Place save(Place place) {
        PlaceJpaEntity entity = mapper.toEntity(place);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Page<Place> search(SearchUseCase.SearchQuery query, Pageable pageable) {
        return jpaRepository.search(query.type(), query.text(), pageable)
                .map(mapper::toDomain);
    }
}
```

Le domaine appelle `PlaceRepository.save(place)`. L'adapter :
1. Mappe `Place` → `PlaceJpaEntity`
2. Sauvegarde via JPA
3. Mappe le résultat `PlaceJpaEntity` → `Place`
4. Retourne le domaine enrichi (avec l'ID généré, timestamps, etc.)

---

## Schéma de dépendances

```
domain/port/out/PlaceRepository  (interface)
        ▲
        │ implémente
PlaceRepositoryAdapter           (@Component)
        │ dépend de
        ├── PlaceJpaRepository   (Spring Data)
        └── PlaceMapper          (MapStruct @Component)
                │ mappe entre
                ├── domain/model/Place
                └── entity/PlaceJpaEntity
```

---

## À retenir

- Les entités JPA et les modèles domaine sont **deux représentations distinctes** du même concept. Ne pas les fusionner.
- MapStruct résout les différences de structure (ex: `Coordinates` ↔ `latitude/longitude`).
- Les adapters sont intentionnellement fins : ils ne contiennent pas de logique métier, seulement du mapping et de la délégation.
- `@JdbcTypeCode(SqlTypes.JSON)` fonctionne avec H2 (dev) et PostgreSQL (prod) sans changer de code.