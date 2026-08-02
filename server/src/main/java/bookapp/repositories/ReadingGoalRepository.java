package bookapp.repositories;

import bookapp.entities.ReadingGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ReadingGoalRepository extends JpaRepository<ReadingGoal, Long> {
    Optional<ReadingGoal> findByUserIdAndTargetYear(Long userId, Integer targetYear);
}
