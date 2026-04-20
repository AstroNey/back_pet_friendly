package lns.back.backend_pet_friendly.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void register_returnsTokensAnd201() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> body = Map.of("email", email, "password", "password123", "name", "Test");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyString())))
                .andExpect(jsonPath("$.expiresIn", greaterThan(0)))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void register_duplicateEmail_returnsError() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.com";
        Map<String, Object> body = Map.of("email", email, "password", "password123", "name", "Dup");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_seededAdmin_returns200WithToken() throws Exception {
        Map<String, Object> body = Map.of("email", "admin@petfriendly.fr", "password", "admin123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.user.email").value("admin@petfriendly.fr"));
    }

    @Test
    void login_wrongPassword_returns4xx() throws Exception {
        Map<String, Object> body = Map.of("email", "admin@petfriendly.fr", "password", "wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        Map<String, Object> body = Map.of("email", "not-an-email", "password", "password123", "name", "Test");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        Map<String, Object> body = Map.of("email", "x@y.z", "password", "123", "name", "Test");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validToken_issuesNewPairAndRevokesOld() throws Exception {
        String email = "ref-" + UUID.randomUUID() + "@test.com";
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "name", "Ref"))))
                .andExpect(status().isCreated())
                .andReturn();
        String refresh = objectMapper.readTree(reg.getResponse().getContentAsString()).get("refreshToken").asText();

        MvcResult res = mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyString())))
                .andReturn();
        String rotated = objectMapper.readTree(res.getResponse().getContentAsString()).get("refreshToken").asText();
        assertThat(rotated).isNotEqualTo(refresh);

        mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", refresh))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refresh_invalidToken_returns4xx() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", "definitely-not-a-jwt"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void logout_thenRefresh_fails() throws Exception {
        String email = "out-" + UUID.randomUUID() + "@test.com";
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "password123", "name", "Out"))))
                .andExpect(status().isCreated())
                .andReturn();
        String refresh = objectMapper.readTree(reg.getResponse().getContentAsString()).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout").param("refreshToken", refresh))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").param("refreshToken", refresh))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void logout_unknownToken_stillReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout").param("refreshToken", "unknown-token"))
                .andExpect(status().isNoContent());
    }
}
