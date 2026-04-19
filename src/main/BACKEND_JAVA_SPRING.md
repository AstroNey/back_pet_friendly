# PetFriendly — Backend Java 21 / Spring Boot 3 (Architecture Hexagonale)

## 1. Vue d'ensemble

**PetFriendly** est une app Flutter de découverte de lieux pet-friendly en France.  
Le backend doit exposer une API REST JSON consommée par l'app mobile.

**Base URL cible** : `http://localhost:8080/api/v1` (dev) → `https://api.petfriendly.fr/api/v1` (prod)

---

## 2. Stack technique

| Composant | Choix |
|-----------|-------|
| **JDK** | Java 21 (LTS) — records, pattern matching, virtual threads |
| **Framework** | Spring Boot 3.3.x |
| **Base de données** | PostgreSQL 16 (+ PostGIS pour les requêtes géospatiales) |
| **ORM** | Spring Data JPA / Hibernate 6 |
| **Sécurité** | Spring Security 6 + JWT (jjwt 0.12) |
| **Build** | Maven 3.9 (wrapper inclus) |
| **Tests** | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| **Documentation API** | SpringDoc OpenAPI 3 (Swagger UI) |
| **Stockage fichiers** | MinIO (S3-compatible) ou AWS S3 |
| **Migrations DB** | Flyway |
| **Validation** | Jakarta Bean Validation (Hibernate Validator) |
| **Mapping** | MapStruct 1.6 |

---

## 3. Architecture Hexagonale

```
┌─────────────────────────────────────────────────────────┐
│                      DRIVING SIDE                        │
│    REST Controllers / Security Filters / Schedulers      │
│              (Adapters IN / Primaires)                   │
└──────────────────────┬──────────────────────────────────┘
                       │  Ports IN (interfaces)
┌──────────────────────▼──────────────────────────────────┐
│                    DOMAIN / HEXAGONE                     │
│                                                          │
│   Entities  ·  Value Objects  ·  Domain Events           │
│   Use Cases (Application Services)                      │
│   Ports IN  ·  Ports OUT                                │
└──────────────────────┬──────────────────────────────────┘
                       │  Ports OUT (interfaces)
┌──────────────────────▼──────────────────────────────────┐
│                     DRIVEN SIDE                          │
│  JPA Repositories / S3 Adapter / Mail Adapter / FCM      │
│              (Adapters OUT / Secondaires)                │
└─────────────────────────────────────────────────────────┘
```

### Règles fondamentales

- Le **domaine ne dépend de rien** (pas de Spring, pas de JPA).
- Les **ports** sont des interfaces Java définies dans le domaine.
- Les **adapters** implémentent les ports et vivent hors du domaine.
- Les **use cases** orchestrent la logique métier via les ports.

---

## 4. Structure du projet

```
petfriendly-backend/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/fr/petfriendly/
    │   │   │
    │   │   ├── domain/                          # Hexagone pur — 0 dépendance framework
    │   │   │   ├── model/
    │   │   │   │   ├── Place.java               # Aggregate root
    │   │   │   │   ├── Review.java              # Entity
    │   │   │   │   ├── User.java                # Aggregate root
    │   │   │   │   ├── Notification.java        # Entity
    │   │   │   │   ├── Coordinates.java         # Value Object (record)
    │   │   │   │   ├── PlaceType.java           # Enum
    │   │   │   │   ├── AnimalType.java          # Enum
    │   │   │   │   └── NotificationType.java    # Enum
    │   │   │   │
    │   │   │   ├── port/
    │   │   │   │   ├── in/                      # Ports primaires (use cases)
    │   │   │   │   │   ├── PlaceUseCase.java
    │   │   │   │   │   ├── SearchUseCase.java
    │   │   │   │   │   ├── ReviewUseCase.java
    │   │   │   │   │   ├── FavoriteUseCase.java
    │   │   │   │   │   ├── AuthUseCase.java
    │   │   │   │   │   ├── UserUseCase.java
    │   │   │   │   │   └── NotificationUseCase.java
    │   │   │   │   └── out/                     # Ports secondaires (driven)
    │   │   │   │       ├── PlaceRepository.java
    │   │   │   │       ├── ReviewRepository.java
    │   │   │   │       ├── UserRepository.java
    │   │   │   │       ├── FavoriteRepository.java
    │   │   │   │       ├── NotificationRepository.java
    │   │   │   │       ├── FileStoragePort.java
    │   │   │   │       ├── TokenPort.java
    │   │   │   │       └── NotificationSenderPort.java
    │   │   │   │
    │   │   │   └── service/                     # Use case implementations
    │   │   │       ├── PlaceService.java
    │   │   │       ├── SearchService.java
    │   │   │       ├── ReviewService.java
    │   │   │       ├── FavoriteService.java
    │   │   │       ├── AuthService.java
    │   │   │       ├── UserService.java
    │   │   │       └── NotificationService.java
    │   │   │
    │   │   ├── infrastructure/                  # Adapters — dépendent du framework
    │   │   │   │
    │   │   │   ├── persistence/                 # Adapter OUT — JPA
    │   │   │   │   ├── entity/
    │   │   │   │   │   ├── PlaceJpaEntity.java
    │   │   │   │   │   ├── ReviewJpaEntity.java
    │   │   │   │   │   ├── UserJpaEntity.java
    │   │   │   │   │   ├── FavoriteJpaEntity.java
    │   │   │   │   │   └── NotificationJpaEntity.java
    │   │   │   │   ├── repository/
    │   │   │   │   │   ├── PlaceJpaRepository.java
    │   │   │   │   │   ├── ReviewJpaRepository.java
    │   │   │   │   │   ├── UserJpaRepository.java
    │   │   │   │   │   ├── FavoriteJpaRepository.java
    │   │   │   │   │   └── NotificationJpaRepository.java
    │   │   │   │   ├── mapper/
    │   │   │   │   │   └── (MapStruct mappers JPA ↔ Domain)
    │   │   │   │   └── adapter/
    │   │   │   │       ├── PlaceRepositoryAdapter.java
    │   │   │   │       ├── ReviewRepositoryAdapter.java
    │   │   │   │       ├── UserRepositoryAdapter.java
    │   │   │   │       ├── FavoriteRepositoryAdapter.java
    │   │   │   │       └── NotificationRepositoryAdapter.java
    │   │   │   │
    │   │   │   ├── security/                    # Adapter IN — Spring Security
    │   │   │   │   ├── JwtTokenAdapter.java     # impl TokenPort
    │   │   │   │   ├── JwtAuthFilter.java
    │   │   │   │   ├── SecurityConfig.java
    │   │   │   │   └── UserDetailsServiceAdapter.java
    │   │   │   │
    │   │   │   ├── storage/                     # Adapter OUT — S3 / MinIO
    │   │   │   │   └── S3FileStorageAdapter.java
    │   │   │   │
    │   │   │   └── notification/                # Adapter OUT — FCM
    │   │   │       └── FcmNotificationAdapter.java
    │   │   │
    │   │   └── web/                             # Adapters IN — REST
    │   │       ├── controller/
    │   │       │   ├── AuthController.java
    │   │       │   ├── PlaceController.java
    │   │       │   ├── ReviewController.java
    │   │       │   ├── FavoriteController.java
    │   │       │   ├── UserController.java
    │   │       │   └── NotificationController.java
    │   │       ├── dto/
    │   │       │   ├── request/
    │   │       │   │   ├── LoginRequest.java
    │   │       │   │   ├── RegisterRequest.java
    │   │       │   │   ├── CreatePlaceRequest.java
    │   │       │   │   ├── CreateReviewRequest.java
    │   │       │   │   ├── SearchRequest.java
    │   │       │   │   └── UpdateProfileRequest.java
    │   │       │   └── response/
    │   │       │       ├── AuthResponse.java
    │   │       │       ├── PlaceResponse.java
    │   │       │       ├── PlaceDetailResponse.java
    │   │       │       ├── ReviewResponse.java
    │   │       │       ├── UserResponse.java
    │   │       │       ├── NotificationResponse.java
    │   │       │       └── PageResponse.java
    │   │       ├── mapper/
    │   │       │   └── (MapStruct mappers Domain ↔ DTO)
    │   │       └── exception/
    │   │           ├── GlobalExceptionHandler.java
    │   │           ├── ResourceNotFoundException.java
    │   │           └── BusinessException.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       ├── application-dev.yml
    │       ├── application-prod.yml
    │       └── db/migration/
    │           ├── V1__init_schema.sql
    │           ├── V2__add_postgis.sql
    │           └── V3__seed_data.sql
    │
    └── test/
        └── java/fr/petfriendly/
            ├── domain/service/          # Tests unitaires — sans Spring
            ├── infrastructure/          # Tests Testcontainers
            └── web/controller/          # Tests @WebMvcTest
```

---

## 5. Modèle de domaine

### 5.1 Entités et Value Objects

```java
// Coordinates.java — Value Object immuable
public record Coordinates(double latitude, double longitude) {
    public Coordinates {
        if (latitude < -90 || latitude > 90) throw new IllegalArgumentException("Invalid latitude");
        if (longitude < -180 || longitude > 180) throw new IllegalArgumentException("Invalid longitude");
    }

    // Distance en km (formule Haversine)
    public double distanceTo(Coordinates other) { ... }
}

// Place.java — Aggregate Root
public class Place {
    private UUID id;
    private String name;
    private PlaceType type;
    private String address;
    private Coordinates coordinates;
    private double rating;           // calculé
    private int reviewCount;         // calculé
    private List<AnimalType> animals;
    private String imageUrl;
    private List<String> galleryUrls;
    private String description;
    private Map<String, String> hours; // { "lundi": "9h-18h", ... }
    private UUID ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Review> reviews;    // chargé à la demande

    // Méthode domaine
    public void addReview(Review review) {
        this.reviews.add(review);
        recalculateRating();
    }

    private void recalculateRating() {
        this.rating = reviews.stream()
            .mapToDouble(Review::rating)
            .average().orElse(0.0);
        this.reviewCount = reviews.size();
    }
}

// Review.java — Entity
public class Review {
    private UUID id;
    private UUID placeId;
    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;
    private double rating;           // 1.0 à 5.0
    private String text;
    private Instant createdAt;
}

// User.java — Aggregate Root
public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private String name;
    private String avatarUrl;
    private List<String> pets;       // ["Labrador", "Chat persan"]
    private Instant createdAt;
    private boolean enabled;
}

// PlaceType.java
public enum PlaceType { RESTAURANT, CAFE, HOTEL, PARC, COMMERCE }

// AnimalType.java
public enum AnimalType { DOG, CAT, OTHER }

// NotificationType.java
public enum NotificationType { NEW_PLACE, NEW_REVIEW, FAVORITE_SALE, SYSTEM, REMINDER }
```

---

## 6. Ports (interfaces du domaine)

### 6.1 Ports IN (Use Cases)

```java
// PlaceUseCase.java
public interface PlaceUseCase {
    Place getById(UUID id);
    Place create(CreatePlaceCommand command);
    Page<Place> list(int page, int size);
}

// SearchUseCase.java
public interface SearchUseCase {
    Page<Place> search(SearchQuery query);
}

// SearchQuery.java — record paramètre de recherche
public record SearchQuery(
    String text,
    PlaceType type,
    List<AnimalType> animals,
    Coordinates userLocation,
    double radiusKm,
    int page,
    int size
) {}

// ReviewUseCase.java
public interface ReviewUseCase {
    List<Review> getByPlace(UUID placeId);
    Review create(UUID placeId, CreateReviewCommand command);
}

// FavoriteUseCase.java
public interface FavoriteUseCase {
    List<Place> getUserFavorites(UUID userId);
    void toggle(UUID userId, UUID placeId);
    boolean isFavorite(UUID userId, UUID placeId);
}

// AuthUseCase.java
public interface AuthUseCase {
    AuthResult login(String email, String password);
    AuthResult register(RegisterCommand command);
    void logout(String token);
}

// UserUseCase.java
public interface UserUseCase {
    User getById(UUID id);
    User updateProfile(UUID id, UpdateProfileCommand command);
    UserStats getStats(UUID id);
}

// NotificationUseCase.java
public interface NotificationUseCase {
    List<Notification> getUserNotifications(UUID userId);
    void markAsRead(UUID notificationId, UUID userId);
    void delete(UUID notificationId, UUID userId);
    void clearAll(UUID userId);
}
```

### 6.2 Ports OUT (Driven)

```java
// PlaceRepository.java
public interface PlaceRepository {
    Optional<Place> findById(UUID id);
    Place save(Place place);
    Page<Place> findAll(Pageable pageable);
    Page<Place> search(SearchQuery query);   // délègue à PostGIS
}

// UserRepository.java
public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    User save(User user);
}

// FavoriteRepository.java
public interface FavoriteRepository {
    List<UUID> findPlaceIdsByUserId(UUID userId);
    void add(UUID userId, UUID placeId);
    void remove(UUID userId, UUID placeId);
    boolean exists(UUID userId, UUID placeId);
}

// FileStoragePort.java
public interface FileStoragePort {
    String upload(byte[] data, String filename, String contentType);
    void delete(String url);
}

// TokenPort.java
public interface TokenPort {
    String generate(UUID userId, String email);
    UUID extractUserId(String token);
    boolean isValid(String token);
}

// NotificationSenderPort.java
public interface NotificationSenderPort {
    void sendPush(UUID userId, String title, String body);
}
```

---

## 7. API REST — Endpoints

### 7.1 Authentification

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `POST` | `/api/v1/auth/register` | Non | Création de compte |
| `POST` | `/api/v1/auth/login` | Non | Connexion → JWT |
| `POST` | `/api/v1/auth/logout` | JWT | Invalidation token |
| `POST` | `/api/v1/auth/refresh` | JWT | Renouvellement token |

**POST /auth/register**
```json
// Request
{
  "email": "user@example.com",
  "password": "Passw0rd!",
  "name": "Jean Dupont",
  "pets": ["Labrador", "Chat"]
}
// Response 201
{
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "user": { "id": "uuid", "email": "...", "name": "...", "pets": [...], "createdAt": "..." }
}
```

**POST /auth/login**
```json
// Request
{ "email": "user@example.com", "password": "Passw0rd!" }
// Response 200
{
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn": 3600,
  "user": { ... }
}
```

---

### 7.2 Lieux (Places)

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/v1/places` | Non | Liste paginée |
| `GET` | `/api/v1/places/{id}` | Non | Détail d'un lieu |
| `POST` | `/api/v1/places` | JWT | Créer un lieu |
| `GET` | `/api/v1/places/search` | Non | Recherche avancée |
| `POST` | `/api/v1/places/{id}/image` | JWT | Upload image principale |
| `POST` | `/api/v1/places/{id}/gallery` | JWT | Upload image galerie |

**GET /places** — Query params : `page`, `size`, `lat`, `lng`, `radius`

```json
// Response 200
{
  "content": [
    {
      "id": "uuid",
      "name": "Le Café des Chats",
      "type": "CAFE",
      "address": "12 rue de la Paix, 75001 Paris",
      "distance": 1.2,
      "rating": 4.5,
      "reviewCount": 23,
      "animals": ["DOG", "CAT"],
      "imageUrl": "https://...",
      "coordinates": { "latitude": 48.8566, "longitude": 2.3522 }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8
}
```

**GET /places/search** — Query params : `q`, `type`, `animals` (répétable), `lat`, `lng`, `radius`, `page`, `size`

```
GET /api/v1/places/search?q=restaurant&animals=DOG&animals=CAT&lat=48.8566&lng=2.3522&radius=5
```

**POST /places**
```json
// Request (multipart/form-data)
{
  "name": "La Brasserie du Chien",
  "type": "RESTAURANT",
  "address": "5 avenue Victor Hugo, 69001 Lyon",
  "latitude": 45.7640,
  "longitude": 4.8357,
  "animals": ["DOG"],
  "description": "...",
  "hours": { "lundi": "12h-14h, 19h-22h", "mardi": "fermé" }
}
// Response 201
{ ...PlaceDetailResponse }
```

**GET /places/{id}**
```json
// Response 200
{
  "id": "uuid",
  "name": "...",
  "type": "RESTAURANT",
  "address": "...",
  "rating": 4.3,
  "reviewCount": 15,
  "animals": ["DOG", "CAT"],
  "imageUrl": "...",
  "galleryUrls": ["...", "..."],
  "description": "...",
  "hours": { "lundi": "9h-18h" },
  "coordinates": { "latitude": 48.8566, "longitude": 2.3522 },
  "reviews": [ { "id": "...", "authorName": "...", "rating": 5, "text": "...", "createdAt": "..." } ],
  "isFavorite": false
}
```

---

### 7.3 Avis (Reviews)

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/v1/places/{id}/reviews` | Non | Liste des avis |
| `POST` | `/api/v1/places/{id}/reviews` | JWT | Créer un avis |
| `DELETE` | `/api/v1/reviews/{id}` | JWT | Supprimer son avis |

**POST /places/{id}/reviews**
```json
// Request
{ "rating": 4.5, "text": "Super endroit, chien très bien accueilli !" }
// Response 201
{ "id": "uuid", "authorName": "Jean", "rating": 4.5, "text": "...", "createdAt": "..." }
```

---

### 7.4 Favoris

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/v1/users/favorites` | JWT | Mes favoris |
| `POST` | `/api/v1/users/favorites/{placeId}` | JWT | Ajouter aux favoris |
| `DELETE` | `/api/v1/users/favorites/{placeId}` | JWT | Retirer des favoris |

---

### 7.5 Profil utilisateur

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/v1/users/me` | JWT | Mon profil + stats |
| `PUT` | `/api/v1/users/me` | JWT | Modifier profil |
| `POST` | `/api/v1/users/me/avatar` | JWT | Upload avatar |

**GET /users/me**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "name": "Jean Dupont",
  "avatarUrl": "...",
  "pets": ["Labrador"],
  "createdAt": "2024-01-15T10:00:00Z",
  "stats": {
    "placesAdded": 3,
    "reviewsWritten": 12,
    "favoritesCount": 7
  }
}
```

---

### 7.6 Notifications

| Méthode | Endpoint | Auth | Description |
|---------|----------|------|-------------|
| `GET` | `/api/v1/notifications` | JWT | Mes notifications |
| `PATCH` | `/api/v1/notifications/{id}/read` | JWT | Marquer comme lu |
| `DELETE` | `/api/v1/notifications/{id}` | JWT | Supprimer |
| `DELETE` | `/api/v1/notifications` | JWT | Tout supprimer |

---

## 8. Schéma base de données (PostgreSQL + PostGIS)

```sql
-- V1__init_schema.sql

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS postgis;

-- Types ENUM
CREATE TYPE place_type AS ENUM ('RESTAURANT', 'CAFE', 'HOTEL', 'PARC', 'COMMERCE');
CREATE TYPE animal_type AS ENUM ('DOG', 'CAT', 'OTHER');
CREATE TYPE notification_type AS ENUM ('NEW_PLACE', 'NEW_REVIEW', 'FAVORITE_SALE', 'SYSTEM', 'REMINDER');

-- Users
CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email        VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name         VARCHAR(100) NOT NULL,
    avatar_url   TEXT,
    pets         TEXT[],              -- tableau ["Labrador", "Chat"]
    enabled      BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Places
CREATE TABLE places (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name         VARCHAR(200) NOT NULL,
    type         place_type NOT NULL,
    address      TEXT NOT NULL,
    location     GEOGRAPHY(POINT, 4326) NOT NULL,  -- PostGIS
    rating       NUMERIC(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    animals      animal_type[] NOT NULL,
    image_url    TEXT,
    gallery_urls TEXT[],
    description  TEXT,
    hours        JSONB,               -- { "lundi": "9h-18h", ... }
    owner_id     UUID REFERENCES users(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index géospatial (recherche par distance)
CREATE INDEX places_location_idx ON places USING GIST(location);
CREATE INDEX places_type_idx ON places(type);

-- Reviews
CREATE TABLE reviews (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    place_id     UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    author_id    UUID NOT NULL REFERENCES users(id),
    rating       NUMERIC(3,1) NOT NULL CHECK (rating >= 1 AND rating <= 5),
    text         TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (place_id, author_id)     -- 1 avis par user par lieu
);

-- Favorites (table de jointure)
CREATE TABLE favorites (
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    place_id     UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, place_id)
);

-- Notifications
CREATE TABLE notifications (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         notification_type NOT NULL,
    title        VARCHAR(200) NOT NULL,
    body         TEXT,
    is_read      BOOLEAN DEFAULT FALSE,
    payload      JSONB,               -- données contextuelles (place_id, etc.)
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Refresh tokens
CREATE TABLE refresh_tokens (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token        TEXT UNIQUE NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 9. Sécurité — JWT

### Flux d'authentification

```
Client → POST /auth/login → AuthController → AuthService
       → UserRepository.findByEmail()
       → BCrypt.verify(password, hash)
       → TokenPort.generate(userId, email)
       → Retourne { token (15min), refreshToken (7j) }

Client → GET /places (Authorization: Bearer <token>)
       → JwtAuthFilter.doFilterInternal()
       → TokenPort.isValid(token)
       → TokenPort.extractUserId(token)
       → Inject SecurityContext
       → PlaceController
```

### JwtAuthFilter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final TokenPort tokenPort;
    private final UserDetailsServiceAdapter userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        if (tokenPort.isValid(token)) {
            UUID userId = tokenPort.extractUserId(token);
            UserDetails userDetails = userDetailsService.loadByUserId(userId);
            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
```

### SecurityConfig

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/places/**").permitAll()
                .requestMatchers("/api/v1/swagger-ui/**", "/api/v1/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

---

## 10. Recherche géospatiale (PostGIS)

La requête clé du projet — trouver des lieux dans un rayon donné autour de l'utilisateur :

```java
// PlaceJpaRepository.java
@Query(value = """
    SELECT p.* FROM places p
    WHERE ST_DWithin(
        p.location,
        ST_MakePoint(:lng, :lat)::geography,
        :radiusMeters
    )
    AND (:type IS NULL OR p.type = CAST(:type AS place_type))
    AND (:text IS NULL OR (
        p.name ILIKE '%' || :text || '%'
        OR p.address ILIKE '%' || :text || '%'
        OR p.description ILIKE '%' || :text || '%'
    ))
    ORDER BY ST_Distance(p.location, ST_MakePoint(:lng, :lat)::geography)
    LIMIT :size OFFSET :offset
    """, nativeQuery = true)
List<PlaceJpaEntity> searchNearby(
    @Param("lat") double lat,
    @Param("lng") double lng,
    @Param("radiusMeters") double radiusMeters,
    @Param("type") String type,
    @Param("text") String text,
    @Param("size") int size,
    @Param("offset") int offset
);
```

---

## 11. Configuration — application.yml

```yaml
# application.yml (commun)
spring:
  application:
    name: petfriendly-backend
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway gère le schéma
    open-in-view: false
  flyway:
    locations: classpath:db/migration

server:
  port: 8080
  servlet:
    context-path: /

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html

petfriendly:
  jwt:
    secret: ${JWT_SECRET}         # min 256 bits
    expiration-minutes: 15
    refresh-expiration-days: 7
  storage:
    bucket: petfriendly-images
    endpoint: ${S3_ENDPOINT}

---
# application-dev.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/petfriendly_dev
    username: petfriendly
    password: petfriendly
  jpa:
    show-sql: true

---
# application-prod.yml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
```

---

## 12. pom.xml — dépendances

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.3.4</spring-boot.version>
    <mapstruct.version>1.6.2</mapstruct.version>
    <jjwt.version>0.12.6</jjwt.version>
    <testcontainers.version>1.20.1</testcontainers.version>
</properties>

<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>

    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>

    <!-- OpenAPI / Swagger -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.6.0</version>
    </dependency>

    <!-- AWS S3 / MinIO -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>2.26.0</version>
    </dependency>

    <!-- Tests -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>${testcontainers.version}</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <release>21</release>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

---

## 13. Gestion des erreurs

### Format d'erreur unifié

```json
{
  "timestamp": "2024-11-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Place not found with id: 550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/v1/places/550e8400-e29b-41d4-a716-446655440000"
}
```

### GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(ErrorResponse.of(404, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(joining(", "));
        return ResponseEntity.status(400).body(ErrorResponse.of(400, message, req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(403).body(ErrorResponse.of(403, "Access denied", req.getRequestURI()));
    }
}
```

---

## 14. Stratégie de tests

### 14.1 Tests unitaires — domaine (sans Spring)

```java
// PlaceServiceTest.java
class PlaceServiceTest {
    PlaceRepository placeRepository = mock(PlaceRepository.class);
    NotificationSenderPort notificationSender = mock(NotificationSenderPort.class);
    PlaceService service = new PlaceService(placeRepository, notificationSender);

    @Test
    void should_recalculate_rating_after_review() {
        Place place = PlaceFixture.standard();
        when(placeRepository.findById(place.getId())).thenReturn(Optional.of(place));

        service.addReview(place.getId(), new CreateReviewCommand(userId, 4.0, "Top !"));

        assertThat(place.getRating()).isEqualTo(4.0);
        verify(placeRepository).save(place);
    }
}
```

### 14.2 Tests d'intégration — Testcontainers

```java
@SpringBootTest
@Testcontainers
class PlaceRepositoryAdapterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgis/postgis:16-3.4")
        .withDatabaseName("petfriendly_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    PlaceRepository placeRepository;

    @Test
    void should_find_places_within_radius() {
        // given: 2 places, l'une à 1km, l'autre à 10km de Paris
        // when: search radius 5km
        // then: only the 1km place is returned
    }
}
```

### 14.3 Tests contrôleurs — @WebMvcTest

```java
@WebMvcTest(PlaceController.class)
class PlaceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PlaceUseCase placeUseCase;

    @Test
    @WithMockUser
    void should_return_place_detail() throws Exception {
        when(placeUseCase.getById(any())).thenReturn(PlaceFixture.standard());

        mockMvc.perform(get("/api/v1/places/{id}", UUID.randomUUID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").exists());
    }
}
```

---

## 15. Ordre d'implémentation recommandé

| Étape | Ce qu'on implémente | Priorité |
|-------|---------------------|----------|
| **1** | Domaine : entités + ports (sans aucun framework) | Critique |
| **2** | Schéma DB + Flyway migrations (V1, V2) | Critique |
| **3** | Auth : register + login + JWT filter | Critique |
| **4** | CRUD Places (sans géolocalisation) | Critique |
| **5** | Recherche géospatiale PostGIS | Haute |
| **6** | Reviews (create + list) | Haute |
| **7** | Favoris | Haute |
| **8** | Profil utilisateur + stats | Moyenne |
| **9** | Upload images (S3/MinIO) | Moyenne |
| **10** | Notifications | Basse |
| **11** | Push FCM | Basse |
| **12** | Tests d'intégration complets | Continue |

---

## 16. Déploiement Docker

### 16.1 Dockerfile (multi-stage build)

```dockerfile
# ── Stage 1 : build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copier uniquement le pom et télécharger les dépendances (cache layer)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copier les sources et builder le JAR
COPY src/ src/
RUN ./mvnw package -DskipTests -q

# ── Stage 2 : runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Sécurité : user non-root
RUN addgroup -S petfriendly && adduser -S petfriendly -G petfriendly
USER petfriendly

# Copier uniquement le JAR final
COPY --from=builder /app/target/*.jar app.jar

# Virtual threads (Java 21) + GC optimisé conteneur
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.threads.virtual.enabled=true"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

### 16.2 Docker Compose — développement local

```yaml
# docker-compose.yml
name: petfriendly-dev

services:
  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_DB: petfriendly_dev
      POSTGRES_USER: petfriendly
      POSTGRES_PASSWORD: petfriendly
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U petfriendly -d petfriendly_dev"]
      interval: 10s
      timeout: 5s
      retries: 5

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minio
      MINIO_ROOT_PASSWORD: minio123
    ports:
      - "9000:9000"   # API S3
      - "9001:9001"   # Console web → http://localhost:9001
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 10s
      timeout: 5s
      retries: 5

  sonarqube:
    image: sonarqube:10-community
    depends_on:
      sonar-db:
        condition: service_healthy
    environment:
      SONAR_JDBC_URL: jdbc:postgresql://sonar-db:5432/sonar
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar
    ports:
      - "9090:9000"   # UI → http://localhost:9090  (admin / admin)
    volumes:
      - sonar_data:/opt/sonarqube/data
      - sonar_logs:/opt/sonarqube/logs
      - sonar_extensions:/opt/sonarqube/extensions
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
    profiles:
      - sonar   # démarrer avec: docker-compose --profile sonar up

  sonar-db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: sonar
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar
    volumes:
      - sonar_db:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U sonar -d sonar"]
      interval: 10s
      timeout: 5s
      retries: 5
    profiles:
      - sonar

  # Optionnel : lancer le backend dans Docker aussi (dev)
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/petfriendly_dev
      SPRING_DATASOURCE_USERNAME: petfriendly
      SPRING_DATASOURCE_PASSWORD: petfriendly
      JWT_SECRET: dev-secret-change-in-production-min-32-chars
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: minio
      S3_SECRET_KEY: minio123
    depends_on:
      postgres:
        condition: service_healthy
      minio:
        condition: service_healthy
    profiles:
      - full

volumes:
  postgres_data:
  minio_data:
  sonar_data:
  sonar_logs:
  sonar_extensions:
  sonar_db:
```

**Commandes dev :**
```bash
# Infra seule (DB + MinIO) — backend dans l'IDE
docker-compose up -d

# Avec SonarQube (première fois : attendre ~90s le démarrage)
docker-compose --profile sonar up -d
# UI : http://localhost:9090  →  login: admin / admin  (changer au 1er login)

# Tout (infra + SonarQube + API compilée)
docker-compose --profile sonar --profile full up -d

# Swagger UI : http://localhost:8080/swagger-ui.html
# MinIO console : http://localhost:9001  (minio / minio123)
```

---

### 16.3 Docker Compose — production

```yaml
# docker-compose.prod.yml
name: petfriendly-prod

services:
  postgres:
    image: postgis/postgis:16-3.4
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - backend
    # Ne PAS exposer le port en prod — accès interne uniquement

  minio:
    image: minio/minio:latest
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${S3_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${S3_SECRET_KEY}
    volumes:
      - minio_data:/data
    networks:
      - backend

  api:
    image: petfriendly-api:${IMAGE_TAG:-latest}
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      S3_ENDPOINT: http://minio:9000
      S3_ACCESS_KEY: ${S3_ACCESS_KEY}
      S3_SECRET_KEY: ${S3_SECRET_KEY}
      FCM_SERVER_KEY: ${FCM_SERVER_KEY}
    depends_on:
      - postgres
      - minio
    networks:
      - backend
      - frontend
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  nginx:
    image: nginx:1.27-alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/certs:/etc/nginx/certs:ro  # certificats TLS
      - nginx_logs:/var/log/nginx
    depends_on:
      - api
    networks:
      - frontend

volumes:
  postgres_data:
  minio_data:
  nginx_logs:

networks:
  backend:
    internal: true   # pas d'accès internet direct
  frontend:
```

---

### 16.4 Configuration Nginx (reverse proxy + TLS)

```nginx
# nginx/nginx.conf
events { worker_connections 1024; }

http {
    # Rate limiting — protection brute-force sur /auth/login
    limit_req_zone $binary_remote_addr zone=auth:10m rate=5r/m;
    limit_req_zone $binary_remote_addr zone=api:10m  rate=60r/m;

    # Redirection HTTP → HTTPS
    server {
        listen 80;
        server_name api.petfriendly.fr;
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name api.petfriendly.fr;

        ssl_certificate     /etc/nginx/certs/fullchain.pem;
        ssl_certificate_key /etc/nginx/certs/privkey.pem;
        ssl_protocols       TLSv1.3 TLSv1.2;
        ssl_ciphers         HIGH:!aNULL:!MD5;

        # Headers sécurité
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        add_header X-Frame-Options DENY;
        add_header X-Content-Type-Options nosniff;

        # Route auth — rate limiting strict
        location /api/v1/auth/login {
            limit_req zone=auth burst=3 nodelay;
            proxy_pass http://api:8080;
            proxy_set_header Host              $host;
            proxy_set_header X-Real-IP         $remote_addr;
            proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Toutes les autres routes API
        location /api/v1/ {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://api:8080;
            proxy_set_header Host              $host;
            proxy_set_header X-Real-IP         $remote_addr;
            proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_read_timeout 30s;
            client_max_body_size 10M;   # upload images
        }

        # Swagger UI — désactiver en prod si besoin
        location /swagger-ui/ {
            proxy_pass http://api:8080;
        }
    }
}
```

---

### 16.5 Fichier `.env` (non commité — local / serveur)

```bash
# .env  ← ajouter à .gitignore !
DB_NAME=petfriendly
DB_USER=petfriendly
DB_PASSWORD=CHANGE_ME_STRONG_PASSWORD

JWT_SECRET=CHANGE_ME_MIN_32_CHARS_RANDOM_STRING_HERE

S3_ACCESS_KEY=CHANGE_ME
S3_SECRET_KEY=CHANGE_ME

FCM_SERVER_KEY=CHANGE_ME

IMAGE_TAG=1.0.0
```

---

### 16.6 CI/CD — Build, SonarQube & Deploy (GitHub Actions)

```yaml
# .github/workflows/ci.yml
name: CI — Test + Sonar + Build

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  # ── Job 1 : Tests + analyse SonarQube ──────────────────────────────────────
  test-and-sonar:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0   # requis par SonarQube pour le blame/SCM

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run tests with coverage (JaCoCo)
        run: ./mvnw verify -q

      - name: SonarQube Analysis
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}   # ex: http://monserveur:9090
        run: |
          ./mvnw sonar:sonar \
            -Dsonar.projectKey=petfriendly-backend \
            -Dsonar.host.url=$SONAR_HOST_URL \
            -Dsonar.token=$SONAR_TOKEN \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
            -Dsonar.qualitygate.wait=true   # bloque le pipeline si le Quality Gate échoue

  # ── Job 2 : Build image Docker + push ──────────────────────────────────────
  build-and-push:
    needs: test-and-sonar
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: |
          docker build -t ghcr.io/${{ github.repository }}/petfriendly-api:${{ github.sha }} .
          docker tag ghcr.io/${{ github.repository }}/petfriendly-api:${{ github.sha }} \
                     ghcr.io/${{ github.repository }}/petfriendly-api:latest

      - name: Push to registry
        run: |
          echo "${{ secrets.REGISTRY_PASSWORD }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
          docker push ghcr.io/${{ github.repository }}/petfriendly-api:${{ github.sha }}
          docker push ghcr.io/${{ github.repository }}/petfriendly-api:latest

  # ── Job 3 : Déploiement serveur ─────────────────────────────────────────────
  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy on server via SSH
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.SERVER_HOST }}
          username: ${{ secrets.SERVER_USER }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/petfriendly
            echo "IMAGE_TAG=${{ github.sha }}" >> .env
            docker-compose -f docker-compose.prod.yml pull api
            docker-compose -f docker-compose.prod.yml up -d --no-deps api
            docker system prune -f
```

---

### 16.7 Structure des fichiers Docker sur le serveur

```
/opt/petfriendly/
├── docker-compose.prod.yml
├── .env                        # secrets (600 permissions)
└── nginx/
    ├── nginx.conf
    └── certs/
        ├── fullchain.pem       # Let's Encrypt / Certbot
        └── privkey.pem
```

**Commandes prod :**
```bash
# Premier déploiement
docker-compose -f docker-compose.prod.yml up -d

# Mise à jour de l'API uniquement (zero-downtime)
docker-compose -f docker-compose.prod.yml up -d --no-deps api

# Voir les logs
docker-compose -f docker-compose.prod.yml logs -f api

# Backup base de données
docker exec petfriendly-prod-postgres-1 \
  pg_dump -U $DB_USER $DB_NAME | gzip > backup_$(date +%Y%m%d).sql.gz
```

---

## 17. SonarQube — Configuration et règles

### 17.1 Plugin Maven (pom.xml)

```xml
<!-- Dans <build><plugins> -->
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
    <version>4.0.0.4121</version>
</plugin>

<!-- JaCoCo : couverture de code -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
    <configuration>
        <!-- Exclure les classes sans logique métier de la couverture -->
        <excludes>
            <exclude>**/infrastructure/persistence/entity/**</exclude>
            <exclude>**/web/dto/**</exclude>
            <exclude>**/*Application.class</exclude>
            <exclude>**/config/**</exclude>
        </excludes>
    </configuration>
</plugin>
```

---

### 17.2 sonar-project.properties

```properties
# sonar-project.properties (à la racine du projet)
sonar.projectKey=petfriendly-backend
sonar.projectName=PetFriendly Backend
sonar.projectVersion=1.0

sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.source=21
sonar.java.binaries=target/classes

# Rapport JaCoCo
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml

# Exclusions — classes sans logique (générées, config, DTO purs)
sonar.exclusions=\
  **/infrastructure/persistence/entity/**,\
  **/web/dto/**,\
  **/*Application.java,\
  **/config/**,\
  **/exception/ResourceNotFoundException.java

# Exclusions de couverture (tests d'intégration non comptés en coverage)
sonar.coverage.exclusions=\
  **/infrastructure/**,\
  **/web/controller/**,\
  **/web/dto/**

# Encodage
sonar.sourceEncoding=UTF-8
```

---

### 17.3 Quality Gate — seuils recommandés

À configurer dans l'UI SonarQube (Administration → Quality Gates → Create) :

| Métrique | Condition | Seuil |
|----------|-----------|-------|
| **Coverage** (nouveau code) | `<` | **80 %** |
| **Duplicated Lines** (nouveau code) | `>` | **3 %** |
| **Reliability Rating** | `worse than` | **A** (0 bugs) |
| **Security Rating** | `worse than` | **A** (0 vulnérabilités) |
| **Maintainability Rating** | `worse than` | **A** (dette < 5 min/line) |
| **Security Hotspots Reviewed** | `<` | **100 %** |
| **Blocker Issues** | `>` | **0** |
| **Critical Issues** | `>` | **0** |

> Le pipeline CI bloque (`sonar.qualitygate.wait=true`) si un de ces seuils n'est pas atteint.

---

### 17.4 Règles Sonar appliquées comme patterns de code

Ces règles Sonar sont des **anti-patterns connus**. Le code du projet est écrit pour les satisfaire dès le départ.

#### Sécurité (OWASP / CWE)

| Règle Sonar | Impact | Pattern appliqué dans le code |
|-------------|--------|-------------------------------|
| `S2076` — Command injection | Critical | Jamais de `Runtime.exec()` avec entrée utilisateur |
| `S2083` — Path traversal | Critical | Noms de fichiers sanitisés avant upload S3 |
| `S3330` — HttpOnly cookie | Critical | Cookies avec `HttpOnly=true` et `Secure=true` |
| `S5344` — Weak password hash | Critical | BCrypt coût 12 (jamais MD5/SHA1 pour les passwords) |
| `S5693` — File upload size | Major | `client_max_body_size 10M` dans Nginx + validation Spring |
| `S6437` — Hard-coded credentials | Blocker | 0 secret dans le code — tout en variables d'env |
| `S4507` — Debug en prod | Major | `spring.jpa.show-sql=false` en profil prod |

**Pattern pour l'upload de fichier (évite path traversal) :**
```java
// FileStoragePort impl — sanitize le nom avant S3
private String sanitize(String filename) {
    String name = Paths.get(filename).getFileName().toString(); // strip path
    return UUID.randomUUID() + "_" + name.replaceAll("[^a-zA-Z0-9._-]", "_");
}
```

#### Fiabilité

| Règle Sonar | Impact | Pattern appliqué |
|-------------|--------|-----------------|
| `S2142` — InterruptedException | Major | `Thread.currentThread().interrupt()` dans les catch |
| `S2259` — Null dereference | Critical | `Optional` partout, jamais de `null` retourné depuis les repositories |
| `S1192` — String literals | Minor | Constantes pour les chaînes répétées (`ApiConstants`) |
| `S3655` — Optional.get() sans isPresent | Critical | Toujours `.orElseThrow()` ou `.orElse()`, jamais `.get()` seul |
| `S1874` — Deprecated API | Major | APIs Java 21 utilisées (ex: `SequencedCollection`, virtual threads) |

**Pattern Optional correct :**
```java
// Mauvais (S3655) :
Optional<Place> opt = repo.findById(id);
Place p = opt.get();  // ← NPE potentiel

// Correct :
Place p = repo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Place", id));
```

#### Maintenabilité

| Règle Sonar | Impact | Pattern appliqué |
|-------------|--------|-----------------|
| `S138` — Méthode trop longue | Major | Use cases découpés en méthodes privées < 30 lignes |
| `S1135` — TODO/FIXME | Info | 0 TODO commité — tout tracé en ticket |
| `S107` — Trop de paramètres | Major | Record pour les commandes/queries (ex: `SearchQuery`) |
| `S1481` — Variable inutilisée | Minor | `_` pour les variables intentionnellement ignorées |
| `S3776` — Complexité cognitive | Critical | Seuil ≤ 15 par méthode — extraire les conditions complexes |

**Pattern record pour éviter trop de paramètres (S107) :**
```java
// Mauvais — 7 paramètres (S107 déclenché à 7+)
Place create(String name, PlaceType type, String address,
             double lat, double lng, List<AnimalType> animals,
             UUID ownerId) { ... }

// Correct — record command comme paramètre unique
public record CreatePlaceCommand(
    String name, PlaceType type, String address,
    Coordinates coordinates, List<AnimalType> animals, UUID ownerId
) {}

Place create(CreatePlaceCommand command) { ... }
```

#### Architecture hexagonale — ArchUnit (tests de règles)

SonarQube ne voit pas les dépendances inter-couches, mais **ArchUnit** les vérifie à l'exécution des tests.  
Ajouter la dépendance dans `pom.xml` :

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

```java
// src/test/java/fr/petfriendly/ArchitectureTest.java
@AnalyzeClasses(packages = "fr.petfriendly")
class ArchitectureTest {

    // Règle 1 : le domaine ne dépend d'aucune couche infra ou web
    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..web..");

    // Règle 2 : les controllers ne s'appellent pas entre eux
    @ArchTest
    static final ArchRule controllers_must_not_call_each_other =
        noClasses().that().resideInAPackage("..web.controller..")
            .should().dependOnClassesThat()
            .resideInAPackage("..web.controller..");

    // Règle 3 : les services domaine n'appellent que des ports OUT
    @ArchTest
    static final ArchRule services_only_use_ports =
        classes().that().resideInAPackage("..domain.service..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..domain.model..",
                "..domain.port..",
                "java..",
                "org.slf4j.."
            );

    // Règle 4 : les adapters JPA n'implémentent que des ports OUT
    @ArchTest
    static final ArchRule jpa_adapters_implement_out_ports =
        classes().that().resideInAPackage("..persistence.adapter..")
            .should().implement(
                JavaClass.Predicates.resideInAPackage("..domain.port.out..")
            );
}
```

Ces 4 règles ArchUnit garantissent que **l'architecture hexagonale reste intacte** même si le projet grandit ou qu'un nouveau dev oublie les conventions.

---

### 17.5 Configuration SonarLint (IDE — IntelliJ IDEA)

SonarLint analyse le code en temps réel dans l'IDE, avant même le commit.

1. **Installer** : `File > Settings > Plugins > SonarLint`
2. **Connecter au serveur local** : `Settings > Tools > SonarLint > Add Connection`
   - Type : SonarQube
   - URL : `http://localhost:9090`
   - Token : généré dans SonarQube (`My Account > Security > Generate Token`)
3. **Lier le projet** : sélectionner `petfriendly-backend`

Les règles configurées dans SonarQube (Quality Profile) sont automatiquement synchronisées dans l'IDE.

---

### 17.6 Commandes utiles SonarQube

```bash
# Analyser localement (SonarQube doit tourner sur :9090)
./mvnw verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9090 \
  -Dsonar.token=MON_TOKEN

# Générer le rapport de couverture seul
./mvnw verify

# Voir le rapport HTML JaCoCo
open target/site/jacoco/index.html

# Réinitialiser l'instance SonarQube (reset complet)
docker-compose --profile sonar down -v
docker-compose --profile sonar up -d
```

---

## 18. Sentry — Monitoring et tracing distribué mobile ↔ backend

L'objectif est de relier un crash Flutter à la requête backend qui en est la cause, dans le même trace Sentry.

```
[Flutter] crash ou erreur HTTP
    → envoie sentry-trace + baggage headers dans chaque requête
        → [Spring Boot] reçoit ces headers, continue le trace
            → les deux événements apparaissent liés dans Sentry
```

---

### 18.1 Côté backend — dépendance Maven

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-spring-boot-starter-jakarta</artifactId>
    <version>7.14.0</version>
</dependency>

<!-- Traces de performance sur les requêtes HTTP et JPA -->
<dependency>
    <groupId>io.sentry</groupId>
    <artifactId>sentry-logback</artifactId>
    <version>7.14.0</version>
</dependency>
```

---

### 18.2 Configuration application.yml

```yaml
# application.yml
sentry:
  dsn: ${SENTRY_DSN}
  environment: ${SPRING_PROFILES_ACTIVE:dev}
  release: ${APP_VERSION:local}

  # Performance — capture 10% des transactions en prod, 100% en dev
  traces-sample-rate: ${SENTRY_TRACES_RATE:0.1}

  # Active la propagation de trace vers le mobile (headers sentry-trace + baggage)
  enable-tracing: true

  # Capture automatique des exceptions non gérées
  exception-resolver-order: -2147483647

  # Ignorer les erreurs "normales" qui ne sont pas des bugs
  ignored-exceptions-for-type:
    - org.springframework.security.access.AccessDeniedException
    - fr.petfriendly.web.exception.ResourceNotFoundException

# Dev : pas de Sentry (évite le bruit pendant le dev)
---
spring:
  config:
    activate:
      on-profile: dev
sentry:
  dsn:   # vide = Sentry désactivé
  traces-sample-rate: 0.0
```

---

### 18.3 Adapter Sentry dans la couche web

Sentry Spring Boot Starter s'auto-configure mais il faut personnaliser les événements pour ajouter le contexte métier (userId, placeId...).

```java
// infrastructure/monitoring/SentryUserContext.java
@Component
public class SentryUserContext {

    // Appelé dans JwtAuthFilter après validation du token
    public void setUser(UUID userId, String email) {
        io.sentry.Sentry.setUser(
            io.sentry.protocol.User.fromMap(Map.of(
                "id", userId.toString(),
                "email", email
            ))
        );
    }

    public void clear() {
        io.sentry.Sentry.setUser(null);
    }
}
```

```java
// Enrichir JwtAuthFilter pour alimenter Sentry
@Override
protected void doFilterInternal(...) {
    // ... validation JWT existante ...
    if (tokenPort.isValid(token)) {
        UUID userId = tokenPort.extractUserId(token);
        UserDetails userDetails = userDetailsService.loadByUserId(userId);
        // ...
        sentryUserContext.setUser(userId, userDetails.getUsername()); // ← ajout
    }
    chain.doFilter(request, response);
    sentryUserContext.clear(); // ← nettoyage après la requête
}
```

---

### 18.4 Capture manuelle d'événements métier

```java
// Dans un use case — capturer une erreur métier importante
public Review create(UUID placeId, CreateReviewCommand command) {
    Place place = placeRepository.findById(placeId)
        .orElseThrow(() -> new ResourceNotFoundException("Place", placeId));

    // Exemple : capturer un cas anormal sans bloquer l'utilisateur
    if (place.getReviewCount() > 500) {
        Sentry.captureMessage(
            "Place dépasse 500 avis : " + placeId,
            SentryLevel.WARNING
        );
    }

    // Ajouter du contexte à tous les events de cette transaction
    Sentry.configureScope(scope -> {
        scope.setTag("place.type", place.getType().name());
        scope.setExtra("place.id", placeId.toString());
    });

    // ...
}
```

---

### 18.5 Côté Flutter — propagation du trace ID

Pour que le trace Flutter soit relié au backend, il faut que le client HTTP Flutter envoie les headers Sentry dans chaque requête.

```yaml
# pubspec.yaml (app Flutter)
dependencies:
  sentry_flutter: ^8.9.0
  sentry_dio: ^8.9.0   # si l'app utilise Dio comme client HTTP
```

```dart
// main.dart — initialisation Sentry Flutter
await SentryFlutter.init(
  (options) {
    options.dsn = 'https://VOTRE_DSN_FLUTTER@sentry.io/PROJET_ID';
    options.environment = 'production';
    options.tracesSampleRate = 0.1;
    // Active la propagation des traces vers le backend
    options.enableAutoPerformanceTracing = true;
    options.tracePropagationTargets = [
      'api.petfriendly.fr',       // prod
      'localhost',                 // dev
    ];
  },
  appRunner: () => runApp(MyApp()),
);
```

```dart
// Configuration Dio avec propagation Sentry
// lib/core/services/api_service.dart
final dio = Dio(BaseOptions(baseUrl: ApiConstants.baseUrl));

dio.addSentry(                    // ← injecte sentry-trace + baggage headers
  captureFailedRequests: true,    // capture les erreurs HTTP 4xx/5xx
);
```

**Ce que ça donne dans Sentry :**
- Une transaction Flutter `GET /api/v1/places` visible dans Performance
- Si le backend renvoie une 500, l'erreur Spring Boot est liée à cette transaction
- Tu vois le chemin complet : écran Flutter → requête HTTP → controller → service → repo → erreur SQL

---

### 18.6 Projets Sentry à créer

Sur [sentry.io](https://sentry.io) (plan gratuit suffisant pour démarrer) :

| Projet | Plateforme | Usage |
|--------|-----------|-------|
| `petfriendly-backend` | Java / Spring Boot | Exceptions, perf endpoints |
| `petfriendly-mobile` | Flutter | Crashs, ANR, perf écrans |

Les deux projets dans la même **organisation** Sentry permettent de naviguer entre les traces liées.

---

### 18.7 Ajouter SENTRY_DSN dans les configs

```bash
# .env (prod)
SENTRY_DSN=https://VOTRE_CLE@o0000.ingest.sentry.io/0000000
SENTRY_TRACES_RATE=0.1
APP_VERSION=1.0.0
```

```yaml
# docker-compose.prod.yml — service api
environment:
  SENTRY_DSN: ${SENTRY_DSN}
  SENTRY_TRACES_RATE: ${SENTRY_TRACES_RATE:-0.1}
  APP_VERSION: ${IMAGE_TAG}
```

---

## 19. Variables d'environnement

| Variable | Obligatoire | Description |
|----------|-------------|-------------|
| `DATABASE_URL` | Prod | JDBC URL PostgreSQL |
| `DB_USER` | Prod | Utilisateur DB |
| `DB_PASSWORD` | Prod | Mot de passe DB |
| `JWT_SECRET` | Toujours | Clé secrète JWT (min 32 chars) |
| `S3_ENDPOINT` | Oui | URL MinIO / AWS S3 |
| `S3_ACCESS_KEY` | Oui | Clé accès S3 |
| `S3_SECRET_KEY` | Oui | Clé secrète S3 |
| `FCM_SERVER_KEY` | Non | Clé Firebase (notifications push) |
| `SENTRY_DSN` | Prod | DSN du projet Sentry backend |
| `SENTRY_TRACES_RATE` | Non | Taux de sampling (défaut : 0.1) |
| `APP_VERSION` | Non | Version affichée dans Sentry |

---

## 19. Checklist avant mise en production

**Qualité & architecture**
- [ ] SonarQube Quality Gate : A en Security, Reliability, Maintainability
- [ ] Couverture JaCoCo ≥ 80 % sur le domaine et les use cases
- [ ] Tous les tests ArchUnit passent (architecture hexagonale intacte)
- [ ] 0 issue Blocker ou Critical dans SonarQube
- [ ] 0 TODO/FIXME dans le code commité

**Sécurité**
- [ ] JWT secret fort (>= 256 bits, en variable d'env)
- [ ] Mots de passe hashés BCrypt (coût ≥ 12)
- [ ] HTTPS uniquement (TLS 1.3)
- [ ] CORS configuré pour l'app mobile uniquement
- [ ] Rate limiting sur `/auth/login` (protection bruteforce)
- [ ] Upload fichiers : sanitize nom + limite taille 10 MB
- [ ] 0 secret en dur dans le code (validé par Sonar S6437)

**Infrastructure**
- [ ] Index PostGIS en place (`places_location_idx`)
- [ ] Flyway migrations testées sur dump prod
- [ ] Logs structurés (JSON) avec niveau INFO en prod
- [ ] Health check endpoint (`/actuator/health`)
- [ ] Backup automatique PostgreSQL planifié

**Monitoring**
- [ ] Sentry backend configuré (DSN en variable d'env, Sentry désactivé en dev)
- [ ] Sentry Flutter configuré avec `tracePropagationTargets` pointant vers le backend
- [ ] Traces distribuées vérifiées : un crash Flutter remonte dans Sentry lié à la requête backend
- [ ] `traces-sample-rate` ≤ 0.1 en prod (ne pas saturer le quota Sentry)
- [ ] Alertes Sentry configurées (seuil d'erreurs, nouvelles issues)
