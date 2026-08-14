package bookapp.security.service;

import bookapp.entities.User;
import bookapp.entities.VerificationToken;
import bookapp.enums.Role;
import bookapp.repositories.UserRepository;
import bookapp.repositories.VerificationTokenRepository;
import bookapp.security.dto.AuthResponse;
import bookapp.security.dto.LoginRequest;
import bookapp.security.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class responsible for handling user authentication lifecycle events,
 * including user registration, account verification, token processing, and credential validation.
 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registers a new user account in a disabled state and generates an account verification token.
     *
     * @param request The {@link RegisterRequest} containing requested credentials (username, email, password).
     * @return An {@link AuthResponse} containing the initial registration details or status message.
     * @throws IllegalArgumentException If the username or email is already registered.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        // 1. Save new user (disabled until email verification)
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);

        // 2. Generate and store verification token (expires in 24 hours)
        String tokenString = UUID.randomUUID().toString();
        VerificationToken verificationToken = VerificationToken.builder()
                .token(tokenString)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(verificationToken);

        //TODO: Trigger Email Service to send verification link containing tokenString
        String jwtToken = jwtService.generateToken(savedUser.getUsername(), savedUser.getId());

        return new AuthResponse(jwtToken, savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
    }

    /**
     * Authenticates an existing user and returns a freshly generated JWT token.
     *
     * @param request The {@link LoginRequest} containing login credentials.
     * @return An {@link AuthResponse} record containing the valid JWT string and core user metadata.
     */
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        String jwtToken = jwtService.generateToken(user.getUsername(), user.getId());

        return new AuthResponse(jwtToken, user.getId(), user.getUsername(), user.getEmail());
    }

    /**
     * Validates a verification token string from an activation link, enables the user account,
     * and deletes the consumed token.
     *
     * @param token The token string sent via request parameter.
     * @return {@code true} if verification succeeded and account was enabled; {@code false} if token is invalid or expired.
     */
    @Transactional
    public boolean verifyToken(String token) {
        Optional<VerificationToken> optionalToken = tokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            return false;
        }

        VerificationToken verificationToken = optionalToken.get();

        // Check token expiration
        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(verificationToken);
            return false;
        }

        // Activate user and purge token
        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);

        tokenRepository.delete(verificationToken);

        return true;
    }
}