package bookapp.repositories;

import bookapp.entities.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link Review} persistence.
 * <p>
 * Provides database query methods to fetch book ratings and user reviews,
 * supporting retrieval by book association, user ownership, and duplicate check validations.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    /**
     * Retrieves all user reviews submitted for a specific book.
     * <p>
     * Uses {@link EntityGraph} to eagerly fetch the associated {@code User} entity in a single SQL query,
     * preventing $N+1$ select performance issues when rendering review sections with author details.
     *
     * @param bookId the database ID of the book whose reviews are being retrieved
     * @return a list of {@link Review} entities for the specified book
     */
    @EntityGraph(attributePaths = {"user"})
    List<Review> findByBookId(Long bookId);

    /**
     * Retrieves all reviews written by a specific user across all books.
     *
     * @param userId the database ID of the user whose reviews are being queried
     * @return a list of {@link Review} entities submitted by the specified user
     */
    List<Review> findByUserId(Long userId);

    /**
     * Retrieves a specific review written by a user for a particular book.
     * <p>
     * Useful for checking whether a user has already reviewed a book or for fetching
     * their existing review to allow editing.
     *
     * @param userId the database ID of the authoring user
     * @param bookId the database ID of the target book
     * @return an {@link Optional} containing the {@link Review} if it exists; empty otherwise
     */
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);
}
