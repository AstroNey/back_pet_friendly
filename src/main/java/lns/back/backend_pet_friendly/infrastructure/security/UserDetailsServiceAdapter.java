package lns.back.backend_pet_friendly.infrastructure.security;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class UserDetailsServiceAdapter implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetails loadByUserId(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found: " + userId));
        return build(user);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return build(user);
    }

    private UserDetails build(User user) {
        return new org.springframework.security.core.userdetails.User(
            user.getId().toString(), user.getPasswordHash(), user.isEnabled(),
            true, true, true,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
