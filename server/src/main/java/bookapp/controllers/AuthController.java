package bookapp.controllers;

import bookapp.security.dto.LoginRequest;
import bookapp.security.dto.RegisterRequest;
import bookapp.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Controller responsible for processing authentication and account management requests.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Logs in a user and stores the JWT in an HTTP-only cookie.
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request) {
        String token = authService.login(request).token();

        ResponseCookie cookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(60 * 60 * 24)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Registers a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        authService.register(request);

        return ResponseEntity.ok("Registration successful! Please check your email to verify your account.");
    }

    /**
     * Logs out the user by clearing the JWT cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Verifies a user account using an email verification token.
     */
    @GetMapping("/verify")
    public ResponseEntity<String> verifyAccount(@RequestParam("token") String token) {
        boolean isVerified = authService.verifyToken(token);

        if (isVerified) {
            return ResponseEntity.ok("Account activated successfully! You can now log in.");
        }

        return ResponseEntity.status(BAD_REQUEST)
                .body("Invalid or expired verification token.");
    }
}