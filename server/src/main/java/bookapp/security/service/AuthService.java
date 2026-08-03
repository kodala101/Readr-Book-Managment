package bookapp.security.service;

import bookapp.entities.User;
import bookapp.repositories.UserRepository;
import bookapp.security.dto.AuthResponse;
import bookapp.security.dto.LoginRequest;
import bookapp.security.dto.RegisterRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Business logic service responsible for managing user authentication workflows.
 * <p>
 * Handles account registration, credentials verification, password hashing,
 * and delegating JWT token creation upon successful login or sign-up.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Constructs the authentication service with required dependencies.
     *
     * @param userRepository repository interface for user database operations
     * @param passwordEncoder utility for securely encoding raw passwords
     * @param authenticationManager Spring Security manager used to verify credentials
     * @param jwtService service responsible for JWT generation and validation
     */
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user account in PostgreSQL.
     * <p>
     * Verifies that the requested username and email are unique, securely hashes
     * the password using {@link PasswordEncoder}, persists the new {@link User} entity,
     * and issues a signed JWT token for immediate authentication.
     *
     * @param request data transfer object containing username, email, and raw password
     * @return an {@link AuthResponse} containing the issued JWT token and basic user details
     * @throws RuntimeException if the username or email address is already registered
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new RuntimeException("Username is already taken!");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates existing user credentials against the database.
     * <p>
     * Delegates credential validation to Spring Security's {@link AuthenticationManager}.
     * If validation succeeds, generates and returns a new JWT token.
     *
     * @param request data transfer object containing the user's login credentials
     * @return an {@link AuthResponse} containing the generated JWT token and user details
     * @throws BadCredentialsException if username or password does not match database records
     * @throws RuntimeException if the user record cannot be found after successful authentication
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String token = jwtService.generateToken(user.getUsername(), user.getId());

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail());
    }
}