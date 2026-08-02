package bookapp.repositories;

import bookapp.entities.JournalEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    @EntityGraph(attributePaths = {"book"})
    List<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<JournalEntry> findByUserIdAndBookIdOrderByCreatedAtDesc(Long userId, Long bookId);
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);
}