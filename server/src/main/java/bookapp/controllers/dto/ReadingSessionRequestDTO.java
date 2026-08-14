package bookapp.controllers.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;

/**
 * Data transfer object for logging an active or past reading session.
 */
public record ReadingSessionRequestDTO(
        @NotNull(message = "Minutes read is required.")
        @Min(value = 1, message = "Session duration must be at least 1 minute.")
        Integer minutesRead,

        @Min(value = 0, message = "Start page cannot be negative.")
        Integer startPage,

        @Min(value = 0, message = "End page cannot be negative.")
        Integer endPage,

        @PastOrPresent(message = "Session date cannot be in the future.")
        LocalDateTime sessionDate,

        @NotNull(message = "Book ID is required.")
        Long bookId
) {}