package lns.back.backend_pet_friendly.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "petfriendly.storage.type", havingValue = "local")
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path rootDir;
    private final String publicBaseUrl;

    public LocalFileStorageAdapter(
            @Value("${petfriendly.storage.local.root-dir:./var/storage}") String rootDir,
            @Value("${petfriendly.storage.local.public-base-url:http://localhost:8080/files}") String publicBaseUrl) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/$", "");
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootDir);
            log.info("Local file storage initialised at {}", rootDir);
        } catch (IOException e) {
            log.warn("Could not initialise local storage dir {} ({}): uploads will fall back to a mock URL",
                    rootDir, e.getMessage());
        }
    }

    @Override
    public String upload(byte[] data, String filename, String contentType) {
        String key = buildKey(filename);
        try {
            Files.write(rootDir.resolve(key), data, StandardOpenOption.CREATE_NEW);
            return publicUrl(key);
        } catch (IOException e) {
            log.warn("Local upload failed for {} ({}) — returning mock URL", key, e.getMessage());
            return publicUrl(key);
        }
    }

    @Override
    public void delete(String url) {
        if (url == null) return;
        String prefix = publicBaseUrl + "/";
        if (!url.startsWith(prefix)) return;
        String key = url.substring(prefix.length());
        try {
            Files.deleteIfExists(rootDir.resolve(key));
        } catch (IOException e) {
            log.warn("Local delete failed for {}: {}", key, e.getMessage());
        }
    }

    private String buildKey(String filename) {
        String base = Paths.get(filename == null ? "file" : filename).getFileName().toString();
        String safe = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return UUID.randomUUID() + "_" + safe;
    }

    private String publicUrl(String key) {
        return publicBaseUrl + "/" + key;
    }
}
