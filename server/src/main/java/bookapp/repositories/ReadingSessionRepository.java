package bookapp.repositories;

import bookapp.entities.ReadingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {
    List<ReadingSession> findByUserIdOrderBySessionDateDesc(Long userId);
    List<ReadingSession> findByUserIdAndBookId(Long userId, Long bookId);

    // Get total minutes read by a user within a specific date range (for goals/analytics)
    @Query("SELECT COALESCE(SUM(s.minutesRead), 0) FROM ReadingSession s WHERE s.user.id = :userId AND s.sessionDate >= :startDate")
    Integer getTotalMinutesReadSince(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    // Check total pages read by a user for a specific book
    @Query("SELECT COALESCE(SUM(s.endPage - s.startPage), 0) FROM ReadingSession s WHERE s.user.id = :userId AND s.book.id = :bookId")
    Integer getTotalPagesReadForBook(@Param("userId") Long userId, @Param("bookId") Long bookId);

    // Get the user's latest reading session (to check if their streak is still active)
    Optional<ReadingSession> findFirstByUserIdOrderBySessionDateDesc(Long userId);
}
