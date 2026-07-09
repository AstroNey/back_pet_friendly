package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Profile fields the current user can update")
public record UpdateProfileRequest(
        @Schema(example = "Alice Dupont") @Size(max = 100) String name,
        @Schema(example = "[\"Rex\"]") @Size(max = 20) List<String> pets) {}
