package lns.back.backend_pet_friendly.infrastructure.storage;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import org.springframework.stereotype.Component;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class S3FileStorageAdapter implements FileStoragePort {
    @Override
    public String upload(byte[] data, String filename, String contentType) {
        String safe = sanitize(filename);
        // TODO: wirer AWS SDK S3 / MinIO ici
        return "http://localhost:9000/petfriendly-images/" + safe;
    }
    @Override public void delete(String url) { /* TODO */ }

    private String sanitize(String filename) {
        String name = Paths.get(filename).getFileName().toString();
        return UUID.randomUUID() + "_" + name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
