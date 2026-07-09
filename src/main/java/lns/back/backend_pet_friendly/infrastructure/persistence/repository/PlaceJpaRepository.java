package lns.back.backend_pet_friendly.infrastructure.persistence.repository;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.infrastructure.persistence.entity.PlaceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface PlaceJpaRepository extends JpaRepository<PlaceJpaEntity, UUID> {
    @Query("SELECT p FROM PlaceJpaEntity p WHERE " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:text IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:text,'%')) OR LOWER(p.address) LIKE LOWER(CONCAT('%',:text,'%'))) AND " +
           "(:filterAnimals = FALSE OR EXISTS (SELECT a FROM p.animals a WHERE a IN :animals))")
    Page<PlaceJpaEntity> search(@Param("type") PlaceType type, @Param("text") String text,
                                @Param("filterAnimals") boolean filterAnimals,
                                @Param("animals") List<AnimalType> animals, Pageable pageable);

    @Query("SELECT p FROM PlaceJpaEntity p JOIN FavoriteJpaEntity f ON f.id.placeId = p.id WHERE f.id.userId = :userId")
    List<PlaceJpaEntity> findFavoritesByUserId(@Param("userId") UUID userId);

    long countByOwnerId(UUID ownerId);

    /**
     * PostGIS-native geospatial search — only works against a PostgreSQL instance with the postgis extension.
     * Filters by radius (metres), optional type, optional text, ordered by distance ascending.
     * Uses the pre-computed {@code location} column (generated STORED) so the GIST index backs ST_DWithin.
     */
    @Query(value = """
            SELECT * FROM places p
            WHERE p.location IS NOT NULL
              AND ST_DWithin(
                p.location,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
              AND (CAST(:type AS text) IS NULL OR p.type = CAST(:type AS text))
              AND (CAST(:text AS text) IS NULL
                   OR LOWER(p.name)    LIKE LOWER('%' || CAST(:text AS text) || '%')
                   OR LOWER(p.address) LIKE LOWER('%' || CAST(:text AS text) || '%'))
              AND (CAST(:filterAnimals AS boolean) = FALSE
                   OR EXISTS (SELECT 1 FROM place_animals pa WHERE pa.place_id = p.id AND pa.animal IN (:animals)))
            ORDER BY ST_Distance(
                p.location,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            ) ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM places p
            WHERE p.location IS NOT NULL
              AND ST_DWithin(
                p.location,
                ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                :radiusMeters
            )
              AND (CAST(:type AS text) IS NULL OR p.type = CAST(:type AS text))
              AND (CAST(:text AS text) IS NULL
                   OR LOWER(p.name)    LIKE LOWER('%' || CAST(:text AS text) || '%')
                   OR LOWER(p.address) LIKE LOWER('%' || CAST(:text AS text) || '%'))
              AND (CAST(:filterAnimals AS boolean) = FALSE
                   OR EXISTS (SELECT 1 FROM place_animals pa WHERE pa.place_id = p.id AND pa.animal IN (:animals)))
            """,
            nativeQuery = true)
    Page<PlaceJpaEntity> searchNearby(@Param("lat") double lat,
                                      @Param("lng") double lng,
                                      @Param("radiusMeters") double radiusMeters,
                                      @Param("type") String type,
                                      @Param("text") String text,
                                      @Param("filterAnimals") boolean filterAnimals,
                                      @Param("animals") List<String> animals,
                                      Pageable pageable);
}
