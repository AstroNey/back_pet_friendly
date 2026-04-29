package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Profile fields the current user can update")
public record UpdateProfileRequest(
        @Schema(example = "Alice Dupont") String name,
        @Schema(example = "[\"Rex\"]") List<String> pets) {}
