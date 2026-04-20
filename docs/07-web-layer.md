# Module : Web Layer

Chemin : `web/`

## Rôle

La couche web est la **frontière HTTP** de l'application. Elle :
1. Reçoit les requêtes HTTP et les valide
2. Extrait l'utilisateur courant du contexte de sécurité
3. Appelle le bon Use Case (port IN)
4. Traduit le résultat domaine en réponse JSON

Elle ne contient **aucune logique métier**.

---

## Contrôleurs — `web/controller/`

### Conventions

```java
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceUseCase placeUseCase;   // port IN, jamais PlaceService directement
    private final SearchUseCase searchUseCase;
```

- Injection du **port IN** (interface), pas de l'implémentation.
- `@RequestMapping("/api/v1/...")` — préfixe versionné.
- Lombok `@RequiredArgsConstructor` pour l'injection constructeur.

### Endpoints principaux

| Contrôleur | Méthode | Route | Auth |
|-----------|---------|-------|------|
| `AuthController` | POST | `/auth/register` | Non |
| `AuthController` | POST | `/auth/login` | Non |
| `AuthController` | POST | `/auth/refresh` | Non |
| `PlaceController` | GET | `/places` | Non |
| `PlaceController` | GET | `/places/{id}` | Non |
| `PlaceController` | GET | `/places/search` | Non |
| `PlaceController` | POST | `/places` | Oui |
| `PlaceController` | PUT | `/places/{id}` | Oui |
| `PlaceController` | DELETE | `/places/{id}` | Oui |
| `ReviewController` | GET | `/places/{id}/reviews` | Non |
| `ReviewController` | POST | `/places/{id}/reviews` | Oui |
| `ReviewController` | DELETE | `/reviews/{id}` | Oui |
| `FavoriteController` | GET | `/users/favorites` | Oui |
| `FavoriteController` | POST | `/users/favorites/{placeId}` | Oui |
| `UserController` | GET | `/users/me` | Oui |
| `UserController` | PUT | `/users/me` | Oui |
| `NotificationController` | GET | `/notifications` | Oui |
| `NotificationController` | PATCH | `/notifications/{id}/read` | Oui |
| `NotificationController` | DELETE | `/notifications/{id}` | Oui |

### Exemple complet d'un endpoint authentifié

```java
@PostMapping("/{placeId}/reviews")
public ResponseEntity<ReviewResponse> createReview(
        @PathVariable UUID placeId,
        @Valid @RequestBody CreateReviewRequest request,
        @AuthenticationPrincipal UserDetails userDetails) {

    UUID userId = UUID.fromString(userDetails.getUsername());

    Review review = reviewUseCase.create(new ReviewUseCase.CreateReviewCommand(
        placeId, userId, request.rating(), request.text()
    ));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ReviewResponse.from(review));
}
```

---

## DTOs — `web/dto/`

### Request records

Tous les DTOs de requête sont des `record` Java avec annotations de validation Bean Validation.

```java
public record CreateReviewRequest(
    @NotNull @Min(1) @Max(5) Integer rating,
    @NotBlank @Size(max = 1000) String text
) {}
```

`@Valid` sur le paramètre `@RequestBody` déclenche la validation. En cas d'échec, Spring lance `MethodArgumentNotValidException`, interceptée par `GlobalExceptionHandler`.

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}
```

```java
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String name,
    List<String> pets
) {}
```

### Response records

Les DTOs de réponse sont aussi des `record` avec une méthode de fabrique statique `from(domain)`.

```java
public record PlaceResponse(
    UUID id,
    String name,
    PlaceType type,
    String address,
    CoordinatesResponse coordinates,
    double rating,
    int reviewCount,
    List<AnimalType> acceptedAnimals,
    String imageUrl
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
            place.getId(),
            place.getName(),
            place.getType(),
            place.getAddress(),
            CoordinatesResponse.from(place.getCoordinates()),
            place.getRating(),
            place.getReviewCount(),
            place.getAcceptedAnimals(),
            place.getImageUrl()
        );
    }
}
```

**Pourquoi `from()` dans le DTO ?** Le contrôleur écrit `PlaceResponse.from(place)` — simple, lisible. On n'a pas besoin d'un mapper supplémentaire pour cette conversion.

### `PageResponse<T>` — Pagination

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast()
        );
    }
}
```

Exemple d'usage dans un contrôleur :

```java
@GetMapping
public PageResponse<PlaceResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<Place> places = placeUseCase.list(pageable);
    return PageResponse.from(places.map(PlaceResponse::from));
}
```

---

## Gestion d'erreurs — `web/exception/`

### `GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                        HttpServletRequest request) {
        return ResponseEntity.status(404).body(new ErrorResponse(
            LocalDateTime.now(), 404, "Not Found", ex.getMessage(), request.getRequestURI()
        ));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, ...) {
        return ResponseEntity.status(400).body(...);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, ...) {
        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(...);
    }
}
```

`@RestControllerAdvice` intercepte les exceptions lancées par n'importe quel contrôleur. **Une seule classe** gère tous les cas d'erreur — format de réponse uniforme pour le client Flutter.

Format de réponse d'erreur :
```json
{
  "timestamp": "2026-04-20T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "Place not found with id: abc-123",
  "path": "/api/v1/places/abc-123"
}
```

### Exceptions métier

```java
// Lance un 404
throw new ResourceNotFoundException("Place not found with id: " + id);

// Lance un 400
throw new BusinessException("Vous avez déjà laissé un avis pour ce lieu");

// Lance un 403 (Spring Security le gère nativement)
throw new AccessDeniedException("Action non autorisée");
```

---

## À retenir

- Les contrôleurs sont **fins** : validation + extraction userId + appel use case + mapping réponse. Rien de plus.
- Les `record` pour les DTOs garantissent l'immutabilité et évitent les classes boilerplate avec getters/setters.
- `@Valid` + `GlobalExceptionHandler` = validation automatique avec réponses d'erreur cohérentes.
- La couche web ne connaît pas `infrastructure/`. Tout passe par les ports IN.