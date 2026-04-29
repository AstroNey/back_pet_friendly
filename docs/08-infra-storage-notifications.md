# Infrastructure — Stockage & Notifications

Deux ports OUT du domaine, deux adapters côté infrastructure. Tous deux **dégradent gracieusement** : l'app boot sans MinIO ni Firebase configurés, les opérations passent en mode log.

---

## Stockage de fichiers — `infrastructure/storage/`

### Port OUT (domaine)

```java
public interface FileStoragePort {
    String upload(byte[] data, String filename, String contentType);
    void delete(String url);
}
```

Le domaine ne connaît ni S3, ni bucket, ni credential.

### S3FileStorageAdapter

Implémentation **AWS SDK v2** (`software.amazon.awssdk.services.s3`), compatible MinIO via `endpointOverride` + `pathStyleAccessEnabled`.

```java
@PostConstruct
void init() {
    try {
        this.s3 = S3Client.builder()
            .endpointOverride(URI.create(endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    } catch (Exception e) {
        // S3 indispo : upload retourne une URL mock, delete = no-op
    }
}
```

**Comportement** :
- `upload` : génère une clé `<UUID>_<filename-sanitized>`, `putObject`, retourne l'URL publique. Si S3 indispo ou échec SDK → URL mock retournée (logs warn).
- `delete` : extrait la clé de l'URL, `deleteObject`. Échec silencieux (warn log).
- Sanitisation : caractères non-alphanum/`._-` remplacés par `_`. UUID préfixe pour éviter les collisions.

### Configuration (`application.yml`)

```yaml
petfriendly:
  storage:
    bucket:     ${S3_BUCKET:petfriendly-images}
    endpoint:   ${S3_ENDPOINT:http://localhost:9000}
    access-key: ${S3_ACCESS_KEY:minio}
    secret-key: ${S3_SECRET_KEY:minio123}
```

En prod, `docker-compose.prod.yml` lance un service `minio` + `minio-init` qui crée le bucket et les permissions.

---

## Notifications push — `infrastructure/notification/`

### Port OUT (domaine)

```java
public interface NotificationSenderPort {
    void sendPush(UUID userId, String title, String body, Map<String, String> data);
}
```

### FcmNotificationAdapter

Implémentation **Firebase Admin SDK 9.4.1**. Le bean `FirebaseMessaging` est injecté **`@Autowired(required = false)`** : si Firebase n'est pas configuré (`petfriendly.firebase.service-account-file` absent), `firebaseMessaging` est `null` et l'adapter passe en mode no-op.

```java
@Override
public void sendPush(UUID userId, String title, String body, Map<String, String> data) {
    if (firebaseMessaging == null) {
        log.debug("[fcm-noop] {} — {}: {}", userId, title, body);
        return;
    }
    String token = userRepository.findById(userId)
        .map(UserJpaEntity::getFcmToken).orElse(null);
    if (token == null || token.isBlank()) return;

    Message msg = Message.builder()
        .setToken(token)
        .setNotification(Notification.builder().setTitle(title).setBody(body).build())
        .putAllData(data != null ? data : Map.of())
        .build();
    try {
        firebaseMessaging.send(msg);
    } catch (FirebaseMessagingException e) {
        // best-effort : warn log, ne fait pas échouer le flow domaine
    }
}
```

**Best-effort** : un échec d'envoi (token expiré, FCM indispo) est loggué mais ne propage pas d'exception. Un push perdu doit pouvoir être retenté ; un flow métier ne doit pas planter pour une notification.

### FirebaseConfig

Initialise le `FirebaseApp` à partir d'un fichier service-account JSON. Si le path n'est pas configuré, le bean n'est pas créé → `@Autowired(required=false)` côté adapter retourne null.

```yaml
petfriendly:
  firebase:
    service-account-file: ${FIREBASE_CREDENTIALS_PATH:}   # vide en dev
```

En dev : laissé vide, FCM no-op, l'app boot avec le log `FCM disabled — no petfriendly.firebase.service-account-file configured`.

### FCM token côté utilisateur

`UserJpaEntity.fcmToken` (string nullable). Le client Flutter s'enregistre côté Firebase, récupère son token, l'envoie via `PUT /api/v1/users/me` (champ à ajouter dans `UpdateProfileRequest` si pas encore exposé) ou un endpoint dédié.

---

## Pourquoi cette architecture

- Les ports isolent le domaine : changer S3 → Cloudinary, ou FCM → APNs natif, ne touche que l'adapter.
- Dégradation gracieuse : l'app boot en local sans aucune dépendance externe (MinIO, Firebase). Le frontend Flutter peut développer contre une API totalement fonctionnelle (les uploads retournent des URLs simulées, les push sont logguées).
- Bonne séparation des responsabilités : `NotificationService` (domaine) sauvegarde la notification en DB, l'adapter FCM gère l'envoi push — deux préoccupations distinctes.
