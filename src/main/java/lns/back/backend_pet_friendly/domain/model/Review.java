package lns.back.backend_pet_friendly.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @Builder
public class Review {

    private UUID id;
    private UUID placeId;
    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;
    private double rating;
    private String text;

    /** Statut de modération. Un nouvel avis est {@link ReviewStatus#PENDING} jusqu'à validation admin. */
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    /** Horodatage de la modération (approbation/rejet), null tant que non modéré. */
    private Instant moderatedAt;

    /** Id de l'admin ayant modéré, null tant que non modéré. */
    private UUID moderatedBy;

    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Nom du lieu — champ d'affichage NON persisté, peuplé uniquement pour les vues admin
     * (liste de modération). Null sur les autres lectures.
     */
    private transient String placeName;
}
