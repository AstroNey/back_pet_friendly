package lns.back.backend_pet_friendly.infrastructure.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lns.back.backend_pet_friendly.domain.port.out.TokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenAdapter implements TokenPort {
    @Value("${petfriendly.jwt.secret}") private String secret;
    @Value("${petfriendly.jwt.expiration-minutes:15}") private int accessMinutes;
    @Value("${petfriendly.jwt.refresh-expiration-days:7}") private int refreshDays;

    private SecretKey key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    @Override public String generateAccessToken(UUID userId, String email) {
        return Jwts.builder().subject(userId.toString()).claim("email", email)
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + (long)accessMinutes * 60_000))
                .signWith(key()).compact();
    }
    @Override public String generateRefreshToken(UUID userId) {
        return Jwts.builder().subject(userId.toString()).claim("type","refresh")
                .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + (long)refreshDays * 86_400_000))
                .signWith(key()).compact();
    }
    @Override public UUID extractUserId(String token) {
        return UUID.fromString(Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload().getSubject());
    }
    @Override public boolean isValid(String token) {
        try { Jwts.parser().verifyWith(key()).build().parseSignedClaims(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
