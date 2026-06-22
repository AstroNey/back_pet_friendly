package lns.back.backend_pet_friendly.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConditionalOnProperty(name = "petfriendly.storage.type", havingValue = "local")
public class LocalFileStorageWebConfig implements WebMvcConfigurer {

    private final String rootDirLocation;

    public LocalFileStorageWebConfig(
            @Value("${petfriendly.storage.local.root-dir:./var/storage}") String rootDir) {
        Path resolved = Paths.get(rootDir).toAbsolutePath().normalize();
        this.rootDirLocation = resolved.toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations(rootDirLocation);
    }
}
