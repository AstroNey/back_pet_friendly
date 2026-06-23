package lns.back.backend_pet_friendly.infrastructure.persistence.adapter;
import lns.back.backend_pet_friendly.domain.model.Review;
import lns.back.backend_pet_friendly.domain.model.ReviewStatus;
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
    @Override public Page<Review> findApprovedByPlaceId(UUID placeId, Pageable pageable) { return jpa.findByPlaceIdAndStatus(placeId, ReviewStatus.APPROVED, pageable).map(mapper::toDomain); }
    @Override public Page<Review> findByAuthorId(UUID authorId, Pageable pageable) { return jpa.findByAuthorId(authorId, pageable).map(mapper::toDomain); }
    @Override public Page<Review> findByStatus(ReviewStatus status, Pageable pageable) { return jpa.findByStatus(status, pageable).map(mapper::toDomain); }
    @Override public Optional<Review> findById(UUID id) { return jpa.findById(id).map(mapper::toDomain); }
    @Override public boolean existsByPlaceIdAndAuthorId(UUID placeId, UUID authorId) { return jpa.existsByPlaceIdAndAuthorId(placeId, authorId); }
    @Override public long countApprovedByPlaceId(UUID placeId) { return jpa.countByPlaceIdAndStatus(placeId, ReviewStatus.APPROVED); }
    @Override public long countByAuthorId(UUID authorId) { return jpa.countByAuthorId(authorId); }
    @Override public double averageApprovedRatingByPlaceId(UUID placeId) { return jpa.averageRatingByPlaceIdAndStatus(placeId, ReviewStatus.APPROVED); }
    @Override public Review save(Review review) { return mapper.toDomain(jpa.save(mapper.toEntity(review))); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
}
