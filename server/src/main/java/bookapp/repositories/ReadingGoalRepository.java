package bookapp.repositories;

import bookapp.entities.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link ReadingGoal} persistence.
 * <p>
 * Provides database query methods for accessing and updating yearly user reading targets
 * (e.g., target book count for a specific calendar year).
 */
@Repository
public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {
    /**
     * Retrieves a user's reading goal for a specific target year.
     *
     * @param userId the database ID of the user whose goal is being queried
     * @param targetYear the calendar year of the goal (e.g., 2026)
     * @return an {@link Optional} containing the user's {@link ReadingGoal} if set; empty otherwise
     */
    Optional<ReadingGoal> findByUserIdAndTargetYear(Long userId, Integer targetYear);
}
