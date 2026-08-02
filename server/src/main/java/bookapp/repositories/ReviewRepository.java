package bookapp.repositories;

import bookapp.entities.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Fetches reviews AND eagerly joins the User object in 1 SQL query!
    @EntityGraph(attributePaths = {"user"})
    List<Review> findByBookId(Long bookId);

    List<Review> findByUserId(Long userId);
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);
}
