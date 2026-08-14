package bookapp.repositories;

import bookapp.entities.User;
import bookapp.entities.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing {@link VerificationToken} persistent entities.
 * Provides standard CRUD operations and custom query methods for email verification workflows.
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    /**
     * Retrieves a verification token entity by its unique token string.
     *
     * @param token The UUID token string sent via user email.
     * @return An {@link Optional} containing the found VerificationToken, or empty if non-existent.
     */
    Optional<VerificationToken> findByToken(String token);

    /**
     * Deletes any existing verification token associated with a given user.
     * Useful for clearing old/expired tokens when issuing a new verification request.
     *
     * @param user The {@link User} entity whose verification tokens should be removed.
     */
    void deleteByUser(User user);
}