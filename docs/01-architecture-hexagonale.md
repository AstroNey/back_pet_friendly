# Architecture hexagonale

## Principe

Le domaine métier ne dépend de rien : ni Spring, ni JPA, ni HTTP. Il exprime uniquement les règles du problème à résoudre. Les dépendances vont de l'extérieur vers l'intérieur.

```
[HTTP / REST]   →   [Domaine]   ←   [BDD / JWT / S3 / FCM]
  (web/)            (pur Java)        (infrastructure/)
```

## Structure

```
lns.back.backend_pet_friendly/
├── domain/                     ← Cœur métier (0 dépendance framework)
│   ├── model/                  ← Entités, Value Objects (records), Enums
│   ├── port/
│   │   ├── in/                 ← Use Cases (interfaces appelées par web/)
│   │   └── out/                ← Repositories, TokenPort, FileStoragePort, NotificationSenderPort
│   └── service/                ← Implémentation des Use Cases (@Service autorisé)
├── infrastructure/             ← Adaptateurs techniques
│   ├── persistence/            ← JPA, MapStruct, RepositoryAdapters
│   ├── security/               ← JWT, Spring Security, BCrypt
│   ├── storage/                ← S3 / MinIO
│   └── notification/           ← Firebase FCM
├── web/                        ← Couche HTTP
│   ├── controller/             ← REST endpoints
│   ├── dto/request/            ← Records *Request
│   ├── dto/response/           ← Records *Response
│   └── exception/              ← GlobalExceptionHandler
└── config/                     ← AppConfig, OpenApiConfig, DataSeeder
```

## Inversion de dépendance

Le domaine **définit** l'interface (`PlaceRepository`). L'infrastructure **implémente** (`PlaceRepositoryAdapter`). Spring injecte l'implémentation via le constructeur.

```
PlaceService (domain)
    ↓ dépend de
PlaceRepository (interface, domain/port/out)
    ↑ implémenté par
PlaceRepositoryAdapter (infrastructure/persistence/adapter)
    ↓ délègue à
PlaceJpaRepository (Spring Data JPA)
```

Le domaine ne connaît jamais l'adapter, seulement l'interface.

## Flux d'une requête HTTP

```
POST /api/v1/places
  ▼
PlaceController       (web)        → valide la requête, extrait le userId du SecurityContext
  ▼ appelle
PlaceUseCase          (port IN)
  ▼ implémenté par
PlaceService          (domain)     → règles métier
  ▼ appelle
PlaceRepository       (port OUT)
  ▼ implémenté par
PlaceRepositoryAdapter (infra)     → mappe domain ↔ JPA via MapStruct
  ▼ délègue à
PlaceJpaRepository    (Spring Data) → SQL
```

## Bénéfices concrets

| Bénéfice | Conséquence |
|----------|-------------|
| Tests rapides | `new PlaceService(mockRepo)` — pas de Spring context, ~2s pour 200 tests |
| Stack swappable | Migrer Spring → Quarkus = changer infra/, domaine intact |
| Lisibilité | Un fichier domain/ se lit comme du Java pur |
| Pas d'Anemic Domain | La logique vit dans les objets domaine (`Place.addReview` recalcule la note) |

## Règles ArchUnit (`src/test/java/.../ArchitectureTest.java`)

| Couche | Peut dépendre de | Ne peut pas |
|--------|-----------------|-------------|
| `domain/model` | `java.*` | Spring, JPA, tout |
| `domain/port` | `domain/model`, `Page`/`Pageable` | Annotations Spring, JPA |
| `domain/service` | `domain/*`, slf4j, `@Service`/`@Component`, Spring Security, `Pageable` | JPA, Web |
| `infrastructure` | Tout le domaine + frameworks | `web/` |
| `web/` | `domain/port/in`, `web/dto` | `infrastructure/` directement |

`Page`/`Pageable` (Spring Data) est toléré dans les ports : pragmatisme, pagination = concept de présentation.
