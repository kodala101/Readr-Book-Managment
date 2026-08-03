package bookapp.repositories;

import bookapp.entities.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link Shelf} persistence.
 * <p>
 * Provides database query methods for organizing user book collections, managing
 * custom shelves (e.g., "Favorites", "Want to Read"), and verifying book containment.
 */
@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    /**
     * Retrieves all custom shelves created by a specific user.
     *
     * @param userId the database ID of the user whose shelves are being queried
     * @return a list of {@link Shelf} entities belonging to the specified user
     */
    List<Shelf> findByUserId(Long userId);

    /**
     * Retrieves a specific shelf by its primary key, verifying that it belongs
     * to the specified user for access control and authorization.
     *
     * @param id the primary key database ID of the target shelf
     * @param userId the database ID of the expected user owner
     * @return an {@link Optional} containing the {@link Shelf} if found and owned by the user; empty otherwise
     */
    Optional<Shelf> findByIdAndUserId(Long id, Long userId);

    /**
     * Checks whether a specific book is currently present inside a given user shelf.
     *
     * @param shelfId the primary key database ID of the shelf
     * @param bookId the primary key database ID of the book to check
     * @return {@code true} if the book exists within the specified shelf; {@code false} otherwise
     */
    boolean existsByIdAndBooks_Id(Long shelfId, Long bookId);

    /**
     * Retrieves a user's shelf by its exact name (e.g., "Favorites" or "Want to Read").
     *
     * @param userId the database ID of the shelf owner
     * @param name the case-sensitive name of the shelf to search for
     * @return an {@link Optional} containing the matching {@link Shelf} if it exists; empty otherwise
     */
    Optional<Shelf> findByUserIdAndName(Long userId, String name);
}
