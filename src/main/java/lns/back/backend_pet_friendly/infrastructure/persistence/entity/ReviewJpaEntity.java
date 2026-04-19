package lns.back.backend_pet_friendly.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"place_id","author_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewJpaEntity {
    @Id UUID id;
    @Column(name = "place_id", nullable = false) UUID placeId;
    @Column(name = "author_id", nullable = false) UUID authorId;
    String authorName;
    String authorAvatarUrl;
    @Column(nullable = false) double rating;
    @Column(columnDefinition = "TEXT") String text;
    @Builder.Default Instant createdAt = Instant.now();
}
