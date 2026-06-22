package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.port.out.ReviewRepository;
import lns.back.backend_pet_friendly.infrastructure.persistence.mapper.ReviewMapper;
import lns.back.backend_pet_friendly.infrastructure.persistence.repository.ReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {
    private final ReviewJpaRepository jpa;
    private final ReviewMapper mapper;
    @Override public Page<Review> findByPlaceId(UUID placeId, Pageable pageable) { return jpa.findByPlaceId(placeId, pageable).map(mapper::toDomain); }
    @Override public Optional<Review> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId) { return jpa.existsByPlaceIdAndAuthorId(placeId, authorId); }
    @Override public long countByPlaceId(UUID placeId) { return jpa.countByPlaceId(placeId); }
    @Override public double averageRatingByPlaceId(UUID placeId) { return jpa.averageRatingByPlaceId(placeId); }
    @Override public Review save(Review review) { return mapper.toDomain(jpa.save(mapper.toEntity(review))); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
}
