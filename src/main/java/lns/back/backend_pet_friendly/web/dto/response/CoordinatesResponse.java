package lns.back.backend_pet_friendly.web.dto.response;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
public record CoordinatesResponse(double latitude, double longitude) {
    public static CoordinatesResponse from(Coordinates c) { return c != null ? new CoordinatesResponse(c.latitude(), c.longitude()) : null; }
}
