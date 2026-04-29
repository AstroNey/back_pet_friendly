package lns.back.backend_pet_friendly.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lns.back.backend_pet_friendly.domain.model.AnimalType;
import lns.back.backend_pet_friendly.domain.model.Coordinates;
import lns.back.backend_pet_friendly.domain.model.Place;
import lns.back.backend_pet_friendly.domain.model.PlaceType;
import lns.back.backend_pet_friendly.web.dto.response.PlaceResponse;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceResponseContractTest {

    private static final List<String> EXPECTED_KEYS = List.of(
            "id", "name", "type", "address", "coordinates",
            "rating", "reviewCount", "animals", "imageUrl",
            "galleryUrls", "description", "hours");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializedPlaceResponse_hasExactlyTheExpectedTopLevelKeys() throws Exception {
        Map<String, String> hours = new LinkedHashMap<>();
        hours.put("monday", "9h-18h");
        hours.put("tuesday", "9h-18h");
        hours.put("wednesday", "9h-18h");
        hours.put("thursday", "9h-18h");
        hours.put("friday", "9h-18h");
        hours.put("saturday", "10h-20h");
        hours.put("sunday", "fermé");

        Place place = Place.builder()
                .id(UUID.randomUUID())
                .name("Le Café des Chats")
                .type(PlaceType.CAFE)
                .address("12 Rue de la Paix, 75001 Paris")
                .coordinates(new Coordinates(48.8698, 2.3309))
                .rating(4.5)
                .reviewCount(12)
                .animals(List.of(AnimalType.DOG, AnimalType.CAT))
                .imageUrl("https://example.com/image.jpg")
                .galleryUrls(List.of("https://example.com/g1.jpg", "https://example.com/g2.jpg"))
                .description("Café accueillant les animaux")
                .hours(hours)
                .build();

        PlaceResponse response = PlaceResponse.from(place);
        String json = objectMapper.writeValueAsString(response);
        JsonNode tree = objectMapper.readTree(json);

        Set<String> actualKeys = collectFieldNames(tree);
        assertThat(actualKeys).containsExactlyInAnyOrderElementsOf(EXPECTED_KEYS);
    }

    @Test
    void serializedPlaceResponse_withNullHours_doesNotThrow() throws Exception {
        Place place = Place.builder()
                .id(UUID.randomUUID())
                .name("Parc")
                .type(PlaceType.PARC)
                .address("Rue Botzaris")
                .coordinates(new Coordinates(48.0, 2.0))
                .hours(null)
                .build();

        PlaceResponse response = PlaceResponse.from(place);
        String json = objectMapper.writeValueAsString(response);
        JsonNode tree = objectMapper.readTree(json);

        assertThat(collectFieldNames(tree)).containsExactlyInAnyOrderElementsOf(EXPECTED_KEYS);
        assertThat(tree.get("hours").isNull()).isTrue();
    }

    private static Set<String> collectFieldNames(JsonNode node) {
        Set<String> names = new java.util.LinkedHashSet<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) names.add(it.next());
        return names;
    }
}
