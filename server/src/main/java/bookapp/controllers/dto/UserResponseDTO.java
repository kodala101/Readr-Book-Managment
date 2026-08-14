package bookapp.controllers.dto;

import bookapp.enums.Role;
import java.time.LocalDateTime;

public record UserResponseDTO(
        Long id,
        String username,
        String email,
        Role role,
        LocalDateTime createdAt,
        String bio,
        String avatarUrl,
        boolean enabled
) {}
