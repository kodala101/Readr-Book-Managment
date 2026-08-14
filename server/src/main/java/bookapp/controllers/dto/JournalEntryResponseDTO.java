package bookapp.controllers.dto;

import java.time.LocalDateTime;

public record JournalEntryResponseDTO(
        Long id,
        String title,
        String content,
        Integer pageNumber,
        LocalDateTime createdAt,
        Long userId,
        Long bookId,
        String bookTitle
) {}
