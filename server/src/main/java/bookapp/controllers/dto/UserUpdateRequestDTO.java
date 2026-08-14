package bookapp.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for updating user profile settings.
 */
public record UserUpdateRequestDTO(
        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
        String username,

        @Email(message = "Invalid email format.")
        String email,

        @Size(max = 500, message = "Bio cannot exceed 500 characters.")
        String bio,

        @Size(max = 1000, message = "Avatar URL is too long.")
        String avatarUrl
) {}