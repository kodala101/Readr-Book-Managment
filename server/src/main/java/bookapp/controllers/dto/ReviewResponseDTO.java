package bookapp.controllers.dto;

import java.time.LocalDateTime;

public record ReviewResponseDTO(
        Long id,
        Double rating,
        String content,
        LocalDateTime createdAt,
        Long userId,
        String username,
        Long bookId,
        String bookTitle
) {}
