package lns.back.backend_pet_friendly.config;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
@EntityScan(basePackages = "lns.back.backend_pet_friendly.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "lns.back.backend_pet_friendly.infrastructure.persistence.repository")
public class AppConfig {}
