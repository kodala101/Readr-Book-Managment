package bookapp.security.service;


import bookapp.repositories.UserRepository;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom implementation of Spring Security's {@link UserDetailsService}.
 * <p>
 * Responsible for fetching user entity data from PostgreSQL via {@link UserRepository}
 * and converting it into a Spring Security compliant {@link AppUserDetails} object.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructs the service with the required {@link UserRepository} dependency.
     *
     * @param userRepository repository interface for user database operations
     */
    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Locates the user based on the provided username.
     * <p>
     * Queries the database for a matching {@link bookapp.entities.User}. If found,
     * wraps the user inside an {@link AppUserDetails} instance; otherwise, throws a
     * {@link UsernameNotFoundException}.
     *
     * @param username the username identifying the user whose data is required
     * @return a fully populated {@link UserDetails} instance for Spring Security
     * @throws UsernameNotFoundException if no user is found with the given username
     */
    @Override
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}
