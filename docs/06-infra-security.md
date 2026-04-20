# Module : Infrastructure — Sécurité

Chemin : `infrastructure/security/`

## Vue d'ensemble

```
Requête HTTP
    │
    ▼
JwtAuthFilter              → extrait le Bearer token, valide, peuple SecurityContext
    │
    ▼
SecurityConfig             → définit quelles routes sont publiques / protégées
    │
    ▼
Controller                 → @AuthenticationPrincipal extrait l'userId du contexte
```

---

## `SecurityConfig.java`

Configuration centrale de Spring Security.

```java
@Configuration @EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // coût 12 = ~250ms/hash
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())             // API stateless, pas de CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()    // login/register libre
                .requestMatchers(GET, "/api/v1/places/**").permitAll()  // consultation libre
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

**BCrypt coût 12 :** intentionnellement lent. Un attaquant avec la base de données ne peut pas tester des millions de mots de passe par seconde.

---

## `JwtTokenAdapter.java`

Implémente `TokenPort` (port OUT du domaine) avec la bibliothèque `jjwt`.

```java
@Component
public class JwtTokenAdapter implements TokenPort {

    private static final long ACCESS_EXPIRY  = 15 * 60 * 1000;        // 15 min
    private static final long REFRESH_EXPIRY = 7 * 24 * 60 * 60 * 1000; // 7 jours

    @Override
    public String generateAccessToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRY))
            .signWith(secretKey)
            .compact();
    }

    @Override
    public UUID extractUserId(String token) {
        return UUID.fromString(
            Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token).getPayload().getSubject()
        );
    }
}
```

Le `userId` est le **subject** du JWT. Le token ne contient pas l'email ou le rôle — juste l'UUID. Toute information supplémentaire nécessite un aller-retour BDD.

**Access vs Refresh token :**
- **Access token (15 min)** : envoyé à chaque requête, durée courte pour limiter l'exposition.
- **Refresh token (7 jours)** : stocké côté client, utilisé uniquement sur `/auth/refresh` pour obtenir un nouveau access token sans re-login.

---

## `JwtAuthFilter.java`

Filtre exécuté avant chaque requête HTTP protégée.

```java
@Component @RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenAdapter tokenAdapter;
    private final UserDetailsServiceAdapter userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);  // pas de token → next filter
            return;
        }

        String token = authHeader.substring(7);

        if (tokenAdapter.isValid(token)) {
            UUID userId = tokenAdapter.extractUserId(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
```

Ce filtre **ne bloque pas** les requêtes sans token — il peuple juste le `SecurityContext` si un token valide est présent. C'est `SecurityConfig` qui bloque les routes protégées si le contexte est vide.

---

## `UserDetailsServiceAdapter.java`

Pont entre Spring Security et notre `UserRepository`.

```java
@Component @RequiredArgsConstructor
public class UserDetailsServiceAdapter implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) {
        UserJpaEntity user = userJpaRepository.findById(UUID.fromString(userId))
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User
            .withUsername(userId)
            .password(user.getPasswordHash())
            .authorities("ROLE_USER")
            .build();
    }
}
```

---

## Utiliser l'utilisateur courant dans un contrôleur

```java
@GetMapping("/me")
public UserResponse getMe(@AuthenticationPrincipal UserDetails userDetails) {
    UUID userId = UUID.fromString(userDetails.getUsername());
    return UserResponse.from(userUseCase.getById(userId));
}
```

`@AuthenticationPrincipal` extrait le `UserDetails` du `SecurityContext` peuplé par `JwtAuthFilter`. Le contrôleur n'a jamais besoin de parser le token lui-même.

---

## Flux complet d'une requête authentifiée

```
Client
  │  Authorization: Bearer eyJhbGci...
  ▼
JwtAuthFilter
  ├── extrait le token
  ├── valide la signature + expiration
  ├── extrait userId du subject
  ├── charge UserDetails depuis BDD
  └── peuple SecurityContextHolder
  ▼
SecurityConfig.filterChain
  └── vérifie que SecurityContext n'est pas vide → OK
  ▼
Controller
  └── @AuthenticationPrincipal UserDetails → userId disponible
```

---

## À retenir

- Le token JWT est **signé mais pas chiffré** : son contenu est lisible (base64). Ne jamais y mettre de données sensibles.
- Spring Security est **stateless** ici : aucune session HTTP. Chaque requête est autonome.
- La durée courte du access token (15 min) force le client à utiliser le refresh token régulièrement, ce qui permet de révoquer un accès en invalidant le refresh token côté serveur (à implémenter si besoin).