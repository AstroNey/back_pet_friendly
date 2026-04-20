package lns.back.backend_pet_friendly.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import lns.back.backend_pet_friendly.domain.port.out.FileStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class S3FileStorageAdapter implements FileStoragePort {

    private final String bucket;
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;

    private S3Client s3;

    public S3FileStorageAdapter(
            @Value("${petfriendly.storage.bucket:petfriendly-images}") String bucket,
            @Value("${petfriendly.storage.endpoint:http://localhost:9000}") String endpoint,
            @Value("${petfriendly.storage.access-key:minio}") String accessKey,
            @Value("${petfriendly.storage.secret-key:minio123}") String secretKey) {
        this.bucket = bucket;
        this.endpoint = endpoint;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

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
            log.info("S3 client initialised against {}", endpoint);
        } catch (Exception e) {
            log.warn("Could not initialise S3 client ({}): upload will fall back to a mock URL", e.getMessage());
        }
    }

    @Override
    public String upload(byte[] data, String filename, String contentType) {
        String key = buildKey(filename);
        if (s3 == null) {
            log.debug("[mock-upload] {}", key);
            return publicUrl(key);
        }
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(bucket).key(key).contentType(contentType).build(),
                    RequestBody.fromBytes(data));
            return publicUrl(key);
        } catch (SdkException e) {
            log.warn("S3 upload failed ({}) — returning mock URL", e.getMessage());
            return publicUrl(key);
        }
    }

    @Override
    public void delete(String url) {
        if (s3 == null || url == null) return;
        String key = extractKey(url);
        if (key == null) return;
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("S3 delete failed for {}: {}", key, e.getMessage());
        }
    }

    private String buildKey(String filename) {
        String base = Paths.get(filename == null ? "file" : filename).getFileName().toString();
        String safe = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return UUID.randomUUID() + "_" + safe;
    }

    private String publicUrl(String key) {
        return endpoint.replaceAll("/$", "") + "/" + bucket + "/" + key;
    }

    private String extractKey(String url) {
        String prefix = endpoint.replaceAll("/$", "") + "/" + bucket + "/";
        return url.startsWith(prefix) ? url.substring(prefix.length()) : null;
    }
}
