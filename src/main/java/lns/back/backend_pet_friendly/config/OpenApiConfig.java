package lns.back.backend_pet_friendly.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearerAuth";

    @Bean
    public OpenAPI petFriendlyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Pet-Friendly API")
                        .description("Backend REST API for the Pet-Friendly mobile application — places, reviews, favorites, notifications.")
                        .version("v1")
                        .contact(new Contact().name("Pet-Friendly").email("contact@petfriendly.app"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local dev"),
                        new Server().url("https://api.petfriendly.app").description("Production")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT access token obtained via POST /api/v1/auth/login")));
    }
}
