package bookapp.security.service;


import bookapp.entities.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Custom implementation of Spring Security's {@link UserDetails} interface.
 * <p>
 * Wraps the JPA domain entity {@link User} to bridge application database details
 * directly into Spring Security's authentication engine and security context.
 */
@Getter
@AllArgsConstructor
public class AppUserDetails implements UserDetails {
    private final User user;

    /**
     * Returns the user's granted authorities.
     * <p>
     * Converts the user's assigned {@link bookapp.enums.Role} into a {@link SimpleGrantedAuthority}
     * prefixed with {@code "ROLE_"} (e.g., {@code "ROLE_ADMIN"}) to comply with Spring Security's role standards.
     *
     * @return a collection containing the granted authorities for this user
     */
    @NonNull
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (user.getRole() != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        }

        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /**
     * Retrieves the hashed password stored in the underlying {@link User} entity.
     *
     * @return the encoded password string
     */
    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /**
     * Retrieves the username of the underlying {@link User} entity.
     *
     * @return the unique username
     */
    @NonNull
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Convenience getter to access the wrapped User's ID directly.
     *
     * @return the unique database ID of the user
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Indicates whether the user's account has expired.
     *
     * @return {@code true} (accounts do not expire by default)
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    /**
     * Indicates whether the user is locked or unlocked.
     *
     * @return {@code true} (accounts are not locked by default)
     */
    @Override
    public boolean isAccountNonLocked() { return true; }

    /**
     * Indicates whether the user's credentials (password) have expired.
     *
     * @return {@code true} (credentials do not expire by default)
     */
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Indicates whether the user is enabled or disabled.
     *
     * @return {@code true} (accounts are enabled by default)
     */
    @Override
    public boolean isEnabled() { return user.isEnabled(); }
}
