package lns.back.backend_pet_friendly.domain.exception;

/** Levée quand une entité demandée n'existe pas. Mappée en HTTP 404 par le GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
