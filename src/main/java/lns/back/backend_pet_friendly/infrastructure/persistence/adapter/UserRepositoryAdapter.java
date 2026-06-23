package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.User;
import lns.back.backend_pet_friendly.domain.port.out.UserRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.UserMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;
    private final UserMapper mapper;
    @Override public Optional<User> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Optional<User> findByEmail(String email) { return jpa.findByEmail(email).map(mapper::toDomain); }
    @Override public boolean existsByEmail(String email) { return jpa.existsByEmail(email); }
    @Override public User save(User user) { return mapper.toDomain(jpa.save(mapper.toEntity(user))); }
    @Override public Page<User> findAll(Pageable pageable) { return jpa.findAll(pageable).map(mapper::toDomain); }
    @Override public void deleteById(UUID id) { jpa.deleteById(id); }
}
