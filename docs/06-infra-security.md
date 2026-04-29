# Infrastructure — Sécurité

Chemin : `infrastructure/security/` + table `refresh_tokens` côté persistence.

## Vue d'ensemble

```
Requête HTTP
    ▼
JwtAuthFilter        → extrait Bearer, valide, peuple SecurityContext
    ▼
SecurityConfig       → définit routes publiques / protégées
    ▼
Controller           → @AuthenticationPrincipal extrait userId
```

## SecurityConfig

```java
@Configuration @EnableWebSecurity
public class SecurityConfig {

    @Bean public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);   // ~250 ms / hash
    }

    @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(GET, "/api/v1/places/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/h2-console/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

BCrypt strength **12** (intentionnellement lent contre le brute-force offline).

## Tokens — access + refresh avec rotation

Deux tokens issus à login/register/refresh :

| Token | TTL | Stockage | Usage |
|-------|-----|----------|-------|
| Access | 15 min (900 s) | Client uniquement | Header `Authorization: Bearer ...` à chaque requête |
| Refresh | 7 jours | **Hash SHA-256 en DB** + JWT côté client | POST `/auth/refresh` pour obtenir un nouveau access |

`AuthService.issueTokens()` :
1. Génère `access` JWT (subject=userId, claim `email`, exp 15 min)
2. Génère `refresh` JWT (subject=userId, claim `type=refresh`, exp 7 j)
3. **Hash le refresh** avec `TokenHasher.sha256(refresh)` et persiste un `RefreshToken{userId, tokenHash, expiresAt}`
4. Retourne les deux tokens en clair au client

## Rotation à chaque refresh

`POST /api/v1/auth/refresh` :
1. Valide la signature + expiration JWT via `TokenPort.isValid()`
2. SHA-256 du refresh → cherche le `RefreshToken` correspondant en DB
3. Vérifie `isActive()` (non révoqué, non expiré)
4. **Révoque** l'ancien (`revokedAt = now`)
5. Émet une nouvelle paire access + refresh, persiste le nouveau hash

Conséquence : un refresh token volé devient inutile dès que le légitime utilisateur s'en sert. Replay détecté côté DB.

## Logout

`POST /api/v1/auth/logout` : SHA-256 du refresh fourni, marque `revokedAt` en DB. Le JWT côté client reste valide cryptographiquement, mais il est rejeté à la prochaine tentative de refresh.

## JwtTokenAdapter — implémentation

Implémente `TokenPort` (port OUT du domaine) avec `jjwt 0.12.6`.

```java
@Component
public class JwtTokenAdapter implements TokenPort {

    @Override public String generateAccessToken(UUID userId, String email) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .expiration(new Date(now() + 15 * 60_000))
            .signWith(secretKey)
            .compact();
    }

    @Override public UUID extractUserId(String token) { /* parseSignedClaims */ }
    @Override public boolean isValid(String token) { /* try-catch parse */ }
}
```

Le `userId` est le subject. Le claim `email` évite un round-trip BDD pour les logs/audit. Le token est **signé non chiffré** : ne rien y mettre de sensible.

## JwtAuthFilter

`OncePerRequestFilter` qui peuple le `SecurityContext` si un Bearer valide est présent. **Ne bloque pas** les requêtes sans token — c'est `SecurityConfig` qui décide qui est protégé.

```java
String token = authHeader.substring(7);
if (tokenAdapter.isValid(token)) {
    UUID userId = tokenAdapter.extractUserId(token);
    UserDetails details = userDetailsService.loadUserByUsername(userId.toString());
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
}
chain.doFilter(request, response);
```

## UserDetailsServiceAdapter

Pont Spring Security ↔ `UserJpaRepository`. Charge l'utilisateur par UUID (pas par email — le JWT subject est l'UUID), retourne un `UserDetails` avec rôle `ROLE_USER`.

## Récupérer l'utilisateur dans un controller

```java
@GetMapping("/me")
public UserResponse me(@AuthenticationPrincipal UserDetails details) {
    UUID userId = UUID.fromString(details.getUsername());
    return UserResponse.from(userUseCase.getById(userId));
}
```

## À retenir

- Le JWT est **signé, pas chiffré** : contenu lisible en base64. Aucune donnée sensible.
- API **stateless** : zéro session HTTP, chaque requête autonome.
- **Rotation systématique** des refresh tokens à chaque usage : sécurité contre le replay.
- Configuration TTL : pour l'instant **hardcodé** dans `AuthService` (`ACCESS_TOKEN_TTL_SECONDS = 900`, `REFRESH_TOKEN_TTL_DAYS = 7`). À déplacer vers `application.yml` (`petfriendly.jwt.expiration-minutes`, `petfriendly.jwt.refresh-expiration-days`) si besoin de variabilité par environnement.
