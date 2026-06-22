package lns.back.backend_pet_friendly.domain.exception;

/** Levée quand un utilisateur tente un 2ᵉ avis sur un lieu déjà noté (contrainte unique). */
public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(String message) {
        super(message);
    }
}