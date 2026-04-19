package lns.back.backend_pet_friendly.web.dto.request;
import java.util.List;
public record UpdateProfileRequest(String name, List<String> pets) {}
