package bookapp.controllers;

import bookapp.controllers.dto.UserResponseDTO;
import bookapp.controllers.dto.UserUpdateRequestDTO;
import bookapp.entities.User;
import bookapp.repositories.UserRepository;
import bookapp.security.service.AppUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing user profile information and settings.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        return ResponseEntity.ok(toResponseDTO(user));
    }

    /**
     * Retrieves a public user profile by username.
     */
    @GetMapping("/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        return ResponseEntity.ok(toResponseDTO(user));
    }

    /**
     * Updates the currently authenticated user's profile.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponseDTO> updateCurrentUser(
            @Valid @RequestBody UserUpdateRequestDTO request,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        // Unique Username Check
        if (request.username() != null
                && !request.username().isBlank()
                && !user.getUsername().equalsIgnoreCase(request.username())) {

            if (userRepository.existsByUsername(request.username())) {
                throw new IllegalArgumentException("Username is already taken.");
            }
            user.setUsername(request.username());
        }

        // Unique Email Check
        if (request.email() != null
                && !request.email().isBlank()
                && !user.getEmail().equalsIgnoreCase(request.email())) {

            if (userRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email is already registered.");
            }
            user.setEmail(request.email());
        }

        // Update profile details (Allow empty string to reset bio/avatar)
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        User updatedUser = userRepository.save(user);

        return ResponseEntity.ok(toResponseDTO(updatedUser));
    }

    private User getAuthenticatedUser(AppUserDetails userDetails) {
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getBio(),
                user.getAvatarUrl(),
                user.isEnabled()
        );
    }
}