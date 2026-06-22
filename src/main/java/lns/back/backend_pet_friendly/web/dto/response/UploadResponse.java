package lns.back.backend_pet_friendly.web.dto.response;
import io.swagger.v3.oas.annotations.media.Schema;
public record UploadResponse(
        @Schema(description = "Public URL of the uploaded file", example = "http://localhost:8080/files/<uuid>_photo.jpg")
        String url) {}
