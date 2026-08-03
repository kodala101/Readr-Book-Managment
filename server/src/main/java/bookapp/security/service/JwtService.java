package bookapp.security.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service class responsible for utility operations regarding JSON Web Tokens (JWT).
 * <p>
 * Handles JWT token generation, cryptographic signature verification, parsing payload claims,
 * and checking token expiration and user ownership.
 */
@Service
public class JwtService {
    @Value("${app.jwt.secret:secretKeyMustBeAtLeast32BytesLongForHS256Algo!}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private int jwtExpirationMs;

    /**
     * Constructs a HMAC-SHA cryptographic signing key using the configured secret key string.
     *
     * @return a {@link SecretKey} instance valid for HS256 algorithm signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed, compact JWT string for an authenticated user.
     * <p>
     * Sets the username as the subject, attaches the database user ID as a custom claim,
     * and sets issue and expiration timestamps.
     *
     * @param username the username to set as the token subject
     * @param userId the database ID of the user
     * @return a signed JWT token string
     */
    public String generateToken(String username, Long userId) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the subject (username) from the payload of a valid JWT token.
     *
     * @param token the raw JWT token string
     * @return the username embedded in the token payload
     * @throws JwtException if the token signature is invalid or expired
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Validates a JWT token's signature, structure, and expiration date.
     *
     * @param token the raw JWT token string
     * @return {@code true} if the token signature is authentic and unexpired; {@code false} otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Verifies that the JWT token is mathematically valid and that the subject username
     * matches the currently loading user details.
     *
     * @param token the raw JWT token string
     * @param userDetails the user details loaded from the database
     * @return {@code true} if the token is unexpired, authentic, and belongs to the given user; {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && validateToken(token);
    }
}