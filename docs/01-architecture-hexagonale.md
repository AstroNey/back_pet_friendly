# Architecture hexagonale

## Principe

L'architecture hexagonale (aussi appelée *Ports & Adapters* ou *Clean Architecture*) a un objectif central : **le domaine métier ne dépend de rien**. Ni de Spring, ni de JPA, ni d'HTTP. Il exprime uniquement les règles du problème à résoudre.

Les dépendances vont toujours de l'extérieur vers l'intérieur :

```
[HTTP / REST]       →   [Domaine]   ←   [Base de données / JWT / S3]
  (Web layer)           (pur Java)        (Infrastructure layer)
```

## Structure du projet

```
lns.back.backend_pet_friendly/
│
├── domain/                     ← Cœur métier (0 dépendance framework)
│   ├── model/                  ← Entités, Value Objects, Enums
│   ├── port/
│   │   ├── in/                 ← Ce que l'extérieur peut demander (Use Cases)
│   │   └── out/                ← Ce dont le domaine a besoin (Repositories, services)
│   └── service/                ← Implémentation des Use Cases
│
├── infrastructure/             ← Adaptateurs techniques
│   ├── persistence/            ← JPA, MapStruct, adapters BDD
│   ├── security/               ← JWT, Spring Security
│   ├── storage/                ← S3 / MinIO
│   └── notification/           ← FCM push notifications
│
└── web/                        ← Couche HTTP
    ├── controller/             ← REST endpoints
    ├── dto/                    ← Request / Response records
    └── exception/              ← Gestion d'erreurs globale
```

## Pourquoi ce découpage ?

### Testabilité
Le domaine peut être testé sans Spring, sans base de données. On injecte des mocks des ports OUT. Les tests unitaires sont rapides et ne dépendent d'aucune infrastructure.

### Maintenabilité
Changer la base de données (H2 → PostgreSQL), le stockage (local → S3), ou le fournisseur JWT n'impacte pas le code métier. Seul l'adaptateur change.

### Lisibilité
En ouvrant `domain/service/`, on lit la logique métier pure, sans bruit Spring.

## Le Dependency Inversion Principle en pratique

Le domaine **définit** l'interface `PlaceRepository` (port OUT). L'infrastructure **implémente** cette interface via `PlaceRepositoryAdapter`. Spring injecte l'implémentation dans le service via le constructeur.

```
PlaceService (domain)
    ↓ dépend de
PlaceRepository (interface, domain)
    ↑ implémenté par
PlaceRepositoryAdapter (infrastructure)
    ↓ délègue à
PlaceJpaRepository (Spring Data JPA)
```

Le domaine ne connaît jamais `PlaceRepositoryAdapter`. Il ne voit que l'interface.

## Flux d'une requête HTTP de bout en bout

```
POST /api/v1/places
    │
    ▼
PlaceController           (web) → valide la requête, extrait l'utilisateur courant
    │ appelle
    ▼
PlaceUseCase              (port IN) → interface
    │ implémentée par
    ▼
PlaceService              (domain) → logique métier, règles
    │ appelle
    ▼
PlaceRepository           (port OUT) → interface
    │ implémentée par
    ▼
PlaceRepositoryAdapter    (infra) → traduit domain ↔ JPA entity
    │ délègue à
    ▼
PlaceJpaRepository        (Spring Data JPA) → SQL
```

## Règles à retenir

| Couche | Peut dépendre de | Ne peut pas dépendre de |
|--------|-----------------|------------------------|
| `domain/model` | Rien | Spring, JPA, tout |
| `domain/port` | `domain/model` | Spring, JPA, tout |
| `domain/service` | `domain/model`, `domain/port` | Spring (sauf `@Service`) |
| `infrastructure` | Tout le domaine + frameworks | `web/` |
| `web/` | `domain/port/in`, `web/dto` | `infrastructure/` directement |