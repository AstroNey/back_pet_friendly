package lns.back.backend_pet_friendly.web.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token passed in the body (never in the query string, to keep it out of logs)")
public record RefreshTokenRequest(
        @Schema(description = "The refresh token obtained at login/register or the previous refresh")
        @NotBlank String refreshToken) {}
