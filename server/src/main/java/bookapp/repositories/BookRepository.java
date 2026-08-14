package bookapp.repositories;

import bookapp.entities.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link Book} persistence.
 * <p>
 * Provides built-in CRUD operations and custom database queries for retrieving,
 * searching, and validating book records in PostgreSQL.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    /**
     * Retrieves a book entity matching the specified unique ISBN code.
     *
     * @param isbn the International Standard Book Number to search for
     * @return an {@link Optional} containing the found book, or empty if no book matches the given ISBN
     */
    Optional<Book> findByIsbn(String isbn);

    /**
     * Performs a case-insensitive search for books where either the title or the author
     * contains the specified keyword substring.
     *
     * @param title the search keyword to match against book titles
     * @param author the search keyword to match against author names
     * @return a list of matching {@link Book} entities, or an empty list if no matches are found
     */
    List<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(String title, String author);

    /**
     * Checks whether a book record with the specified ISBN already exists in the database.
     *
     * @param isbn the International Standard Book Number to check
     * @return {@code true} if a book with the given ISBN exists; {@code false} otherwise
     */
    boolean existsByIsbn(String isbn);

    /**
     * Retrieves a paginated list of books with an average rating greater than or equal to
     * the specified minimum threshold.
     *
     * @param minRating the minimum average rating threshold (e.g., 4.0)
     * @param pageable the pagination and sorting information (page number, page size, sort order)
     * @return a {@link Page} containing matching {@link Book} entities, or an empty page if none are found
     */
    Page<Book> findByAverageRatingGreaterThanEqual(Double minRating, Pageable pageable);

    /**
     * Retrieves a paginated list of books that have received at least the specified
     * total number of ratings or reviews.
     *
     * @param minRatingsCount the minimum number of total ratings required (e.g., 100)
     * @param pageable the pagination and sorting information (page number, page size, sort order)
     * @return a {@link Page} containing matching {@link Book} entities, or an empty page if none are found
     */
    Page<Book> findByTotalRatingsGreaterThanEqual(Integer minRatingsCount, Pageable pageable);
}