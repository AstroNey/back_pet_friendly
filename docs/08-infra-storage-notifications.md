# Module : Infrastructure — Stockage & Notifications

---

## Stockage de fichiers — `infrastructure/storage/`

### Port OUT (domaine)

```java
public interface FileStoragePort {
    String upload(String filename, byte[] content, String contentType);
    void delete(String fileUrl);
}
```

Le domaine exprime uniquement ce dont il a besoin : uploader un fichier, récupérer une URL, supprimer. Pas de S3, pas de bucket, pas de credential.

### `S3FileStorageAdapter.java`

```java
@Component
public class S3FileStorageAdapter implements FileStoragePort {

    @Override
    public String upload(String filename, byte[] content, String contentType) {
        // TODO: intégrer AWS SDK / MinIO
        String sanitized = UUID.randomUUID() + "_" + sanitize(filename);
        log.info("Mock upload: {}", sanitized);
        return "https://storage.petfriendly.app/images/" + sanitized;
    }

    @Override
    public void delete(String fileUrl) {
        log.info("Mock delete: {}", fileUrl);
    }

    private String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
```

**État actuel :** implémentation mock (log uniquement). L'URL retournée est simulée.

**Prochaine étape :** remplacer le corps des méthodes par des appels à `software.amazon.awssdk.services.s3.S3Client` ou au SDK MinIO. **Aucune autre classe du projet n'a besoin de changer** — le port isole ce détail.

**Sanitisation du nom :** le prefixe UUID évite les collisions et les caractères dangereux dans les noms de fichiers (injection de chemin, espaces…).

### Configuration prévue

```yaml
# application.yml
storage:
  bucket: petfriendly-images
  endpoint: http://localhost:9000   # MinIO local
  access-key: ${STORAGE_ACCESS_KEY}
  secret-key: ${STORAGE_SECRET_KEY}
```

---

## Notifications push — `infrastructure/notification/`

### Port OUT (domaine)

```java
public interface NotificationSenderPort {
    void sendPush(UUID userId, String title, String body, Map<String, String> payload);
}
```

### `NoOpNotificationSenderAdapter.java`

```java
@Component
public class NoOpNotificationSenderAdapter implements NotificationSenderPort {

    @Override
    public void sendPush(UUID userId, String title, String body, Map<String, String> payload) {
        log.debug("NoOp push notification to user {}: {}", userId, title);
        // TODO: intégrer Firebase FCM
    }
}
```

**État actuel :** no-op (ne fait rien). Les notifications sont sauvegardées en base via `NotificationService`, mais pas envoyées sur le téléphone.

**Prochaine étape :** intégrer Firebase Admin SDK. Le client Flutter s'enregistre avec un FCM token → stocker ce token sur `UserJpaEntity` → `FcmNotificationAdapter` l'utilise pour envoyer.

```java
// Exemple d'intégration future
@Component @RequiredArgsConstructor
public class FcmNotificationAdapter implements NotificationSenderPort {

    private final FirebaseMessaging firebaseMessaging;
    private final UserJpaRepository userRepository;

    @Override
    public void sendPush(UUID userId, String title, String body, Map<String, String> payload) {
        String fcmToken = userRepository.findById(userId)
            .map(UserJpaEntity::getFcmToken)
            .orElseThrow();

        Message message = Message.builder()
            .setToken(fcmToken)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(payload)
            .build();

        firebaseMessaging.send(message);
    }
}
```

Encore une fois : **seul l'adaptateur change**, pas le domaine.

---

## Deux adapters "TODO" — pourquoi les garder ?

Ces implémentations mock permettent à l'application de démarrer et d'être testable **sans infrastructure externe**. Le développement frontend (Flutter) peut avancer sans que S3 ou FCM soient opérationnels.

C'est un avantage direct de l'architecture hexagonale : on peut stub n'importe quel port sans impact sur le reste du code.