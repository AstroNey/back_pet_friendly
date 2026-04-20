package lns.back.backend_pet_friendly.infrastructure.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;

/**
 * Initialises Firebase Admin SDK at startup <em>if</em> a service-account JSON file is reachable
 * via {@code petfriendly.firebase.service-account-file}. Otherwise the bean exposes {@code null}
 * so {@link FcmNotificationAdapter} degrades to a no-op. Production simply drops the JSON file
 * at the configured path (or points the env var at a mounted secret) — no code change needed.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(
            @Value("${petfriendly.firebase.service-account-file:}") String serviceAccountPath) {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.info("FCM disabled — no petfriendly.firebase.service-account-file configured");
            return null;
        }
        File file = new File(serviceAccountPath);
        if (!file.exists()) {
            log.warn("FCM disabled — service-account file not found at {}", serviceAccountPath);
            return null;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("FCM enabled — FirebaseApp initialised from {}", serviceAccountPath);
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.error("FCM disabled — failed to initialise FirebaseApp: {}", e.getMessage());
            return null;
        }
    }
}
