package lns.back.backend_pet_friendly.web.dto.request;
import jakarta.validation.constraints.*;
import java.util.List;
public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min=6) String password, @NotBlank String name, List<String> pets) {}
