package lns.back.backend_pet_friendly.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lns.back.backend_pet_friendly.domain.model.NotificationType;
import lns.back.backend_pet_friendly.domain.port.in.NotificationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class NotificationControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired NotificationUseCase notificationUseCase;

    private String token;
    private UUID userId;

    @BeforeEach
    void setUp() throws Exception {
        String email = "notif-user-" + UUID.randomUUID() + "@test.com";
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "password", "password123", "name", "Notif"))))
                .andExpect(status().isCreated())
                .andReturn();
        var body = objectMapper.readTree(res.getResponse().getContentAsString());
        token = body.get("token").asText();
        userId = UUID.fromString(body.get("user").get("id").asText());
    }

    private UUID seedNotification(String title) {
        return notificationUseCase.create(new NotificationUseCase.CreateNotificationCommand(
                userId, NotificationType.SYSTEM, title, "body", Map.of("k", "v"))).getId();
    }

    @Test
    void list_returnsUserNotifications() throws Exception {
        seedNotification("hello-1");
        seedNotification("hello-2");

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[0].type").value("SYSTEM"))
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void markRead_flipsReadFlag() throws Exception {
        UUID id = seedNotification("mark-me");

        mockMvc.perform(patch("/api/v1/notifications/" + id + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var arr = objectMapper.readTree(listed.getResponse().getContentAsString());
        boolean foundRead = false;
        for (var n : arr) {
            if (n.get("id").asText().equals(id.toString())) {
                foundRead = n.get("read").asBoolean();
                break;
            }
        }
        org.assertj.core.api.Assertions.assertThat(foundRead).isTrue();
    }

    @Test
    void markRead_otherUserNotification_isForbidden() throws Exception {
        String otherEmail = "other-" + UUID.randomUUID() + "@test.com";
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", otherEmail, "password", "password123", "name", "Other"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID otherId = UUID.fromString(
                objectMapper.readTree(reg.getResponse().getContentAsString()).get("user").get("id").asText());

        UUID notifId = notificationUseCase.create(new NotificationUseCase.CreateNotificationCommand(
                otherId, NotificationType.SYSTEM, "not yours", "body", null)).getId();

        mockMvc.perform(patch("/api/v1/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void delete_removesNotification() throws Exception {
        UUID id = seedNotification("delete-me");

        mockMvc.perform(delete("/api/v1/notifications/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_unknownNotification_4xx() throws Exception {
        UUID ghost = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/notifications/" + ghost)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void clearAll_emptiesUserInbox() throws Exception {
        seedNotification("a");
        seedNotification("b");
        seedNotification("c");

        mockMvc.perform(delete("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
