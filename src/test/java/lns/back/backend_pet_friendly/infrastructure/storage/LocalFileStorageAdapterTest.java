package lns.back.backend_pet_friendly.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class LocalFileStorageAdapterTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageAdapter adapter;
    private static final String BASE_URL = "http://test/files";

    @BeforeEach
    void setUp() {
        adapter = new LocalFileStorageAdapter(tempDir.toString(), BASE_URL);
        adapter.init();
    }

    @Test
    void upload_writes_file_and_returns_url_with_prefix() throws IOException {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        String url = adapter.upload(data, "photo.png", "image/png");

        assertThat(url).startsWith(BASE_URL + "/");
        String key = url.substring((BASE_URL + "/").length());
        Path written = tempDir.resolve(key);
        assertThat(Files.exists(written)).isTrue();
        assertThat(Files.readAllBytes(written)).isEqualTo(data);
    }

    @Test
    void delete_removes_file_when_url_matches_prefix() throws IOException {
        byte[] data = "to-delete".getBytes(StandardCharsets.UTF_8);
        String url = adapter.upload(data, "doomed.txt", "text/plain");

        try (Stream<Path> before = Files.list(tempDir)) {
            assertThat(before.count()).isEqualTo(1);
        }

        adapter.delete(url);

        try (Stream<Path> after = Files.list(tempDir)) {
            assertThat(after.count()).isZero();
        }
    }

    @Test
    void delete_with_external_url_is_noop() throws IOException {
        byte[] data = "stay".getBytes(StandardCharsets.UTF_8);
        adapter.upload(data, "keep.txt", "text/plain");

        assertThatCode(() -> adapter.delete("https://other.example/foo.png"))
                .doesNotThrowAnyException();

        try (Stream<Path> after = Files.list(tempDir)) {
            assertThat(after.count()).isEqualTo(1);
        }
    }
}
