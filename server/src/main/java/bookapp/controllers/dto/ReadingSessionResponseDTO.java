package bookapp.controllers.dto;

import java.time.LocalDateTime;

public record ReadingSessionResponseDTO(
        Long id,
        Integer minutesRead,
        Integer startPage,
        Integer endPage,
        LocalDateTime sessionDate,
        Long userId,
        Long bookId,
        String bookTitle
) {}
