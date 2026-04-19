package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name = "places")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceJpaEntity {
    @Id UUID id;
    @Column(nullable = false) String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) PlaceType type;
    @Column(nullable = false) String address;
    Double latitude;
    Double longitude;
    @Builder.Default double rating = 0.0;
    @Builder.Default int reviewCount = 0;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(columnDefinition = "varchar(20)[]") @Builder.Default List<AnimalType> animals = new ArrayList<>();
    String imageUrl;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(columnDefinition = "text[]") @Builder.Default List<String> galleryUrls = new ArrayList<>();
    @Column(columnDefinition = "TEXT") String description;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") Map<String, String> hours;
    UUID ownerId;
    @Builder.Default Instant createdAt = Instant.now();
    @Builder.Default Instant updatedAt = Instant.now();
}
