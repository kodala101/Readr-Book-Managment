package bookapp.repositories;

import bookapp.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for managing {@link User} persistence.
 * <p>
 * Provides database query methods for account authentication, user lookup,
 * and uniqueness validation for usernames and email addresses during registration.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Retrieves a user entity matching the specified username.
     *
     * @param username the unique username to search for
     * @return an {@link Optional} containing the found {@link User}; empty if no user exists with that username
     */
    Optional<User> findByUsername(String username);

    /**
     * Retrieves a user entity matching the specified email address.
     *
     * @param email the unique email address to search for
     * @return an {@link Optional} containing the found {@link User}; empty if no user exists with that email
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user account with the specified username already exists.
     *
     * @param username the username to check for existence
     * @return {@code true} if a user with the given username exists; {@code false} otherwise
     */
    Boolean existsByUsername(String username);

    /**
     * Checks whether a user account with the specified email address already exists.
     *
     * @param email the email address to check for existence
     * @return {@code true} if a user with the given email exists; {@code false} otherwise
     */
    Boolean existsByEmail(String email);
}
