package bookapp.repositories;

import bookapp.entities.Shelf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShelfRepository extends JpaRepository<Shelf, Long> {
    List<Shelf> findByUserId(Long userId);
    Optional<Shelf> findByIdAndUserId(Long id, Long userId);

    // Check if a specific book exists inside a user's shelf
    boolean existsByIdAndBooks_Id(Long shelfId, Long bookId);

    // Find a user's shelf by its name (e.g., "Favorites" or "Want to Read")
    Optional<Shelf> findByUserIdAndName(Long userId, String name);
}
