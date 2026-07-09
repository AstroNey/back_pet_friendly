package lns.back.backend_pet_friendly.web.support;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * Validation des uploads d'images : rejette un fichier vide ou dont le Content-Type n'est pas une
 * image autorisée (jpg/png/webp). Empêche le stockage — puis le service public via /files/** —
 * de contenu HTML/SVG (XSS stocké). La taille est bornée par spring.servlet.multipart.max-file-size.
 */
public final class ImageUploadValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private ImageUploadValidator() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: only JPEG, PNG and WebP images are allowed");
        }
    }
}
