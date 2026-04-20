# Module : Domaine — Modèles

Chemin : `domain/model/`

## Rôle

Les modèles sont le vocabulaire du problème métier. Ils expriment ce qu'est une `Place`, un `User`, une `Review` — indépendamment de comment ils sont stockés ou exposés.

## Aggregates

Un **Aggregate** est un objet métier principal qui possède son identité propre (UUID) et encapsule sa propre logique de modification.

### `Place.java`

Représente un lieu pet-friendly (restaurant, café, hôtel…).

```java
@Builder @Getter @Setter
public class Place {
    private UUID id;
    private String name;
    private PlaceType type;
    private String address;
    private Coordinates coordinates;   // Value Object
    private double rating;
    private int reviewCount;
    private List<AnimalType> acceptedAnimals;
    private String imageUrl;
    private List<String> galleryUrls;
    private Map<String, String> openingHours;
    private UUID ownerId;
    private List<Review> reviews;      // relation lazy
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Logique métier encapsulée :** `Place.addReview(Review)` recalcule automatiquement `rating` et `reviewCount`. La règle de calcul vit ici, pas dans le service.

---

### `User.java`

Compte utilisateur avec liste d'animaux.

```java
@Builder @Getter @Setter
public class User {
    private UUID id;
    private String email;
    private String passwordHash;
    private String name;
    private String avatarUrl;
    private List<String> pets;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

`passwordHash` est stocké BCrypt (coût 12). Le mot de passe en clair ne circule jamais dans le domaine.

---

### `Review.java`

Avis d'un utilisateur sur un lieu. Contrainte métier : **un seul avis par utilisateur par lieu** — vérifiée dans `ReviewService`.

```java
@Builder @Getter @Setter
public class Review {
    private UUID id;
    private UUID placeId;
    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;
    private int rating;          // 1-5
    private String text;
    private LocalDateTime createdAt;
}
```

---

### `Notification.java`

Notification envoyée à un utilisateur.

```java
@Builder @Getter @Setter
public class Notification {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private boolean read;
    private Map<String, String> payload;   // données arbitraires (ex: placeId)
    private LocalDateTime createdAt;
}
```

---

## Value Object

### `Coordinates.java`

Un **Value Object** est immuable et n'a pas d'identité propre — deux coordonnées identiques sont égales.

```java
public record Coordinates(double latitude, double longitude) {
    public Coordinates {
        if (latitude < -90 || latitude > 90)
            throw new IllegalArgumentException("Latitude invalide");
        if (longitude < -180 || longitude > 180)
            throw new IllegalArgumentException("Longitude invalide");
    }

    public double distanceTo(Coordinates other) {
        // formule de Haversine (distance en km)
    }
}
```

`record` en Java 21 génère automatiquement : constructeur, getters, `equals`, `hashCode`, `toString`. Parfait pour les Value Objects.

---

## Enums

### `PlaceType`
```
RESTAURANT, CAFE, HOTEL, PARC, COMMERCE
```

### `AnimalType`
```
DOG, CAT, OTHER
```

### `NotificationType`
```
NEW_PLACE, NEW_REVIEW, FAVORITE_SALE, SYSTEM, REMINDER
```

Les enums sont sérialisés en `String` dans la base (pas en index entier) pour la lisibilité et la stabilité.

---

## À retenir

- Les modèles n'ont **aucune annotation JPA** (`@Entity`, `@Column`…). Ils sont des POJOs purs.
- La logique métier **qui appartient à un objet** est dans l'objet (`Place.addReview`). Le reste est dans les services.
- `record` = Value Object immuable. `class` + Lombok = entité mutable avec identité.