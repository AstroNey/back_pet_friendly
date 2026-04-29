package lns.back.backend_pet_friendly.web.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.List;

@Schema(description = "Payload to register a new user")
public record RegisterRequest(
        @Schema(example = "alice@petfriendly.fr") @Email @NotBlank String email,
        @Schema(example = "secret123", minLength = 6) @NotBlank @Size(min=6) String password,
        @Schema(example = "Alice") @NotBlank String name,
        @Schema(description = "Optional list of pet names", example = "[\"Rex\", \"Whiskers\"]") List<String> pets) {}
