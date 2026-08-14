package bookapp.repositories;

import bookapp.entities.JournalEntry;
import bookapp.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link JournalEntry} persistence.
 * <p>
 * Provides database query methods to fetch personal reading journal entries and reflections,
 * filtered by user ownership and book association, ordered chronologically.
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    /**
     * Retrieves all journal entries created by a specific user, sorted by creation date descending.
     * <p>
     * Uses {@link EntityGraph} to eagerly fetch the associated {@code Book} entity in a single query,
     * avoiding $N+1$ lazy loading select issues.
     *
     * @param userId the database ID of the user whose journal entries are being queried
     * @return a list of {@link JournalEntry} entities ordered from newest to oldest
     */
    @EntityGraph(attributePaths = {"book"})
    List<JournalEntry> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Retrieves all journal entries created by a specific user for a specific book,
     * sorted by creation date descending.
     *
     * @param userId the database ID of the user who wrote the entries
     * @param bookId the database ID of the target book
     * @return a list of matching {@link JournalEntry} entities ordered from newest to oldest
     */
    List<JournalEntry> findByUserIdAndBookIdOrderByCreatedAtDesc(Long userId, Long bookId);

    /**
     * Retrieves a specific journal entry by its primary key, ensuring that it belongs
     * to the specified user for access control validation.
     *
     * @param id the primary key database ID of the journal entry
     * @param userId the database ID of the expected user owner
     * @return an {@link Optional} containing the entry if found and owned by the user; empty otherwise
     */
    Optional<JournalEntry> findByIdAndUserId(Long id, Long userId);

    /**
     * Retrieves all journal entries written by a specific user.
     *
     * @param user The {@link User} entity author.
     * @return A list of journal entries belonging to the user.
     */
    List<JournalEntry> findByUser(User user);
}