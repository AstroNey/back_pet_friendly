package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.Notification;
import lns.back.backend_pet_friendly.domain.port.out.NotificationRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.NotificationMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {
    private final NotificationJpaRepository jpa;
    private final NotificationMapper mapper;
    @Override public List<Notification> findByUserId(UUID userId) { return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toDomain).toList(); }
    @Override public Optional<Notification> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public Notification save(Notification n) { return mapper.toDomain(jpa.save(mapper.toEntity(n))); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
    @Override public void deleteAllByUserId(UUID userId) { jpa.deleteAllByUserId(userId); }
}
