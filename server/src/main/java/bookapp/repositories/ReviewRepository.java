package bookapp.repositories;

import bookapp.entities.Review;
import bookapp.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Retrieves all reviews submitted by a specific user.
     *
     * @param user The {@link User} entity author.
     * @return A list of reviews written by the user.
     */
    List<Review> findByUser(User user);

    /**
     * Calculates the average rating for a given book ID.
     * SQL/JPQL AVG() natively returns a Double (e.g. 4.25).
     * Wrapped in Optional in case the book has 0 reviews.
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Optional<Double> findAverageRatingByBookId(@Param("bookId") Long bookId);

    /**
     * Counts the total number of reviews/ratings for a given book ID.
     */
    int countByBookId(Long bookId);
}

