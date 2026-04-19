package lns.back.backend_pet_friendly.web.dto.request;
import jakarta.validation.constraints.*;
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
