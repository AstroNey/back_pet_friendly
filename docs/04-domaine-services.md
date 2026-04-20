# Module : Domaine — Services

Chemin : `domain/service/`

## Rôle

Les services implémentent les Use Cases (ports IN). C'est ici que vit **la logique métier** : orchestration des appels aux ports OUT, validations, règles de gestion.

Chaque service est annoté `@Service` (le seul point de contact avec Spring) et utilise l'injection par constructeur (`@RequiredArgsConstructor` Lombok).

---

## AuthService

```java
@Service @RequiredArgsConstructor
public class AuthService implements AuthUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPort tokenPort;
}
```

**register** : vérifie que l'email n'existe pas, hash le mot de passe (BCrypt via `PasswordEncoder`), sauvegarde l'utilisateur, génère access + refresh tokens.

**login** : charge l'utilisateur par email, compare le hash avec `passwordEncoder.matches()`, génère les tokens si OK.

**refresh** : valide le refresh token via `TokenPort.isValid()`, extrait le `userId`, génère un nouvel access token.

`PasswordEncoder` est un port OUT implicite (interface Spring Security). `AuthService` n'appelle jamais BCrypt directement.

---

## PlaceService

```java
@Service @RequiredArgsConstructor
public class PlaceService implements PlaceUseCase {
    private final PlaceRepository placeRepository;
    private final FileStoragePort fileStoragePort;
}
```

**create** : mappe `CreatePlaceCommand` → `Place`, génère un UUID, délègue à `placeRepository.save()`.

**uploadImage** : délègue à `FileStoragePort.upload()`, met à jour `place.imageUrl`, sauvegarde.

**delete** : charge le lieu, supprime l'image si elle existe (`fileStoragePort.delete()`), puis supprime l'entité.

---

## ReviewService

```java
@Service @RequiredArgsConstructor
public class ReviewService implements ReviewUseCase {
    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
}
```

**Règle métier clé — un seul avis par utilisateur :**
```java
if (reviewRepository.existsByPlaceIdAndAuthorId(placeId, authorId)) {
    throw new BusinessException("Vous avez déjà laissé un avis pour ce lieu");
}
```

**create** : vérifie l'unicité, construit la `Review`, appelle `place.addReview(review)` pour recalculer la note, sauvegarde le lieu et l'avis.

**delete** : vérifie que l'auteur de l'avis est bien l'utilisateur courant (contrôle d'accès métier).

---

## FavoriteService

```java
@Service @RequiredArgsConstructor
public class FavoriteService implements FavoriteUseCase {
    private final FavoriteRepository favoriteRepository;
    private final PlaceRepository placeRepository;
}
```

**toggle** : si le favori existe → supprime, sinon → ajoute. Vérifie d'abord que le lieu existe (lance `ResourceNotFoundException` sinon).

---

## SearchService

```java
@Service @RequiredArgsConstructor
public class SearchService implements SearchUseCase {
    private final PlaceRepository placeRepository;
}
```

Service très fin : délègue entièrement à `placeRepository.search(query, pageable)`. La logique de filtrage est dans la couche persistence (JPQL ou PostGIS).

---

## UserService

```java
@Service @RequiredArgsConstructor
public class UserService implements UserUseCase {
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final FileStoragePort fileStoragePort;
}
```

**getStats** : agrège les données de plusieurs repositories → retourne un `UserStats` record avec le nombre d'avis, de favoris, etc.

**uploadAvatar** : upload via `FileStoragePort`, puis met à jour `user.avatarUrl`.

---

## NotificationService

```java
@Service @RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {
    private final NotificationRepository notificationRepository;
}
```

Toutes les opérations vérifient que `notification.getUserId().equals(requestingUserId)` avant de modifier ou supprimer — contrôle d'accès au niveau métier.

---

## Principes à retenir

### Les services ne touchent pas à HTTP
Aucun `HttpServletRequest`, `ResponseEntity`, ou annotation REST. Si un service a besoin de l'utilisateur courant, il reçoit son `UUID` en paramètre (passé par le contrôleur).

### Les services ne touchent pas à JPA
Aucun `@Transactional` (sauf cas justifié), aucun `EntityManager`. Toute persistence passe par les interfaces de port OUT.

### Les services lancent des exceptions métier
- `ResourceNotFoundException` → traduite en 404 par le `GlobalExceptionHandler`
- `BusinessException` → traduite en 400
- `AccessDeniedException` → traduite en 403

### Injection par constructeur
```java
// ✓ Correct — constructeur final, testable facilement
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceRepository placeRepository;
}

// ✗ À éviter — injection par champ, non testable sans Spring
@Autowired
private PlaceRepository placeRepository;
```