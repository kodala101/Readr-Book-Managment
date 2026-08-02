package bookapp.repositories;

import bookapp.entities.Quote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    @EntityGraph(attributePaths = {"book"})
    List<Quote> findByUserId(Long userId);
    List<Quote> findByUserIdAndBookId(Long userId, Long bookId);
}
