package lns.back.backend_pet_friendly.domain.model;

/** Statut de modération d'un avis. Un avis n'est public qu'une fois {@link #APPROVED} par un ADMIN. */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED
}
