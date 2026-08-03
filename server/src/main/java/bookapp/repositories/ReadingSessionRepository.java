package bookapp.repositories;

import bookapp.entities.ReadingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link ReadingSession} persistence.
 * <p>
 * Provides database query methods for tracking reading timer sessions, calculating total
 * reading duration statistics, pages read per book, and determining streak activity.
 */
@Repository
public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {
    /**
     * Retrieves all reading sessions logged by a specific user, ordered from newest to oldest.
     *
     * @param userId the database ID of the user whose reading sessions are being queried
     * @return a list of {@link ReadingSession} entities sorted by session date descending
     */
    List<ReadingSession> findByUserIdOrderBySessionDateDesc(Long userId);

    /**
     * Retrieves all reading sessions logged by a specific user for a particular book.
     *
     * @param userId the database ID of the user
     * @param bookId the database ID of the book
     * @return a list of matching {@link ReadingSession} entities
     */
    List<ReadingSession> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Calculates the aggregate total minutes spent reading by a user since a specified start date.
     * <p>
     * Useful for tracking progress toward periodic goals or displaying reading analytics.
     * Returns {@code 0} if no sessions are found within the date range.
     *
     * @param userId the database ID of the user
     * @param startDate the cutoff timestamp from which to start summing session minutes
     * @return the total number of minutes read, or 0 if no reading sessions exist in the timeframe
     */
    @Query("SELECT COALESCE(SUM(s.minutesRead), 0) FROM ReadingSession s WHERE s.user.id = :userId AND s.sessionDate >= :startDate")
    Integer getTotalMinutesReadSince(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    /**
     * Calculates the aggregate total number of pages read by a user for a specific book across all sessions.
     * <p>
     * Returns {@code 0} if no sessions exist for the specified user and book combination.
     *
     * @param userId the database ID of the user
     * @param bookId the database ID of the target book
     * @return the total number of pages read, or 0 if no reading sessions exist for the book
     */
    @Query("SELECT COALESCE(SUM(s.endPage - s.startPage), 0) FROM ReadingSession s WHERE s.user.id = :userId AND s.book.id = :bookId")
    Integer getTotalPagesReadForBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    /**
     * Retrieves the single most recent reading session logged by a specific user.
     * <p>
     * Commonly used to determine whether a user's current daily reading streak remains active.
     *
     * @param userId the database ID of the user
     * @return an {@link Optional} containing the latest {@link ReadingSession} if one exists; empty otherwise
     */
    Optional<ReadingSession> findFirstByUserIdOrderBySessionDateDesc(Long userId);
}
