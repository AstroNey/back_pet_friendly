# Module : Domaine — Ports

Les ports sont les **interfaces** qui définissent comment le domaine communique avec l'extérieur. Il y en a deux catégories opposées.

---

## Ports IN — Ce que l'extérieur peut demander

Chemin : `domain/port/in/`

Un port IN est un **Use Case** : une action que le système sait faire. C'est l'API du domaine vue de l'extérieur (contrôleurs HTTP, jobs, etc.).

### Pattern : Command / Query imbriqués

Chaque interface port IN définit ses propres records en son sein. Ça évite de polluer un package `dto` commun, et ça garde les contrats proches de leur contexte.

```java
public interface AuthUseCase {

    record RegisterCommand(String email, String password, String name, List<String> pets) {}
    record AuthResult(String accessToken, String refreshToken, long expiresIn, User user) {}

    AuthResult register(RegisterCommand command);
    AuthResult login(String email, String password);
    AuthResult refresh(String refreshToken);
}
```

### Inventaire des ports IN

| Interface | Commands / Queries | Méthodes principales |
|-----------|-------------------|---------------------|
| `PlaceUseCase` | `CreatePlaceCommand` | list, getById, create, update, delete, uploadImage |
| `SearchUseCase` | `SearchQuery` | search(query) → Page\<Place\> |
| `ReviewUseCase` | `CreateReviewCommand` | getByPlace, create, delete |
| `FavoriteUseCase` | — | getUserFavorites, toggle, isFavorite |
| `AuthUseCase` | `RegisterCommand`, `AuthResult` | register, login, refresh |
| `UserUseCase` | `UpdateProfileCommand`, `UserStats` | getById, updateProfile, getStats, uploadAvatar |
| `NotificationUseCase` | — | getUserNotifications, markAsRead, delete, clearAll |

### Pourquoi des interfaces ?

Le contrôleur HTTP dépend de `PlaceUseCase` (interface), pas de `PlaceService` (implémentation). Ça permet :
- de tester le contrôleur en mockant l'interface
- de changer l'implémentation sans toucher le contrôleur

---

## Ports OUT — Ce dont le domaine a besoin

Chemin : `domain/port/out/`

Un port OUT représente une **dépendance externe** que le domaine requiert (persistance, token, stockage...). Le domaine définit l'interface ; l'infrastructure fournit l'implémentation.

### Repositories (persistance)

```java
public interface PlaceRepository {
    Optional<Place> findById(UUID id);
    Place save(Place place);
    void delete(UUID id);
    Page<Place> findAll(Pageable pageable);
    Page<Place> search(SearchUseCase.SearchQuery query, Pageable pageable);
}
```

Ces interfaces ignorent totalement JPA, SQL, ou H2. Le domaine ne sait pas comment les données sont stockées.

| Interface | Méthodes notables |
|-----------|------------------|
| `PlaceRepository` | findById, save, delete, findAll, search |
| `UserRepository` | findByEmail, existsByEmail, save |
| `ReviewRepository` | findByPlaceId (paginé), existsByPlaceIdAndAuthorId |
| `FavoriteRepository` | findPlacesByUserId, add, remove, exists |
| `NotificationRepository` | findByUserId, deleteAllByUserId |

### Ports de services techniques

```java
public interface TokenPort {
    String generateAccessToken(UUID userId);
    String generateRefreshToken(UUID userId);
    UUID extractUserId(String token);
    boolean isValid(String token);
}
```

```java
public interface FileStoragePort {
    String upload(String filename, byte[] content, String contentType);
    void delete(String fileUrl);
}
```

```java
public interface NotificationSenderPort {
    void sendPush(UUID userId, String title, String body, Map<String, String> payload);
}
```

`TokenPort` dans le domaine est une décision forte : la logique d'authentification (`AuthService`) peut appeler `generateAccessToken` sans savoir que c'est du JWT. Si on change de mécanisme de token, seul `JwtTokenAdapter` change.

---

## Résumé visuel

```
        ┌─────────────────────────────────────┐
        │            DOMAINE                  │
        │                                     │
  HTTP  │  port/in/        domain/service/    │  port/out/
 ──────►│  PlaceUseCase ◄── PlaceService ────►│  PlaceRepository
        │  (interface)      (implémentation)  │  (interface)
        │                                     │
        └─────────────────────────────────────┘
              ▲                                    │
              │ Controller appelle                 │ Adapter implémente
              │                                    ▼
        PlaceController                   PlaceRepositoryAdapter
          (web layer)                      (infrastructure)
```