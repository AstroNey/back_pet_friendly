package lns.back.backend_pet_friendly.domain.port.out;

public interface FileStoragePort {
    String upload(byte[] data, String filename, String contentType);
    void delete(String url);
}
