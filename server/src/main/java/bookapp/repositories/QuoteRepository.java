package bookapp.repositories;

import bookapp.entities.Quote;
import bookapp.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository interface for managing {@link Quote} persistence.
 * <p>
 * Provides database query methods to retrieve favorite user-saved book quotes and excerpts,
 * filtered by user ownership and book association.
 */
@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    /**
     * Retrieves all quotes saved by a specific user.
     * <p>
     * Uses {@link EntityGraph} to eagerly fetch the associated {@code Book} entity in a single query,
     * preventing $N+1$ select performance issues when rendering quote cards with book details.
     *
     * @param userId the database ID of the user whose quotes are being queried
     * @return a list of {@link Quote} entities belonging to the specified user
     */
    @EntityGraph(attributePaths = {"book"})
    List<Quote> findByUserId(Long userId);

    /**
     * Retrieves all saved quotes created by a specific user for a specific book.
     *
     * @param userId the database ID of the user who saved the quotes
     * @param bookId the database ID of the target book
     * @return a list of matching {@link Quote} entities
     */
    List<Quote> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Retrieves all quotes saved by a specific user.
     *
     * @param user The {@link User} entity owner.
     * @return A list of saved quotes belonging to the user.
     */
    List<Quote> findByUser(User user);
}
