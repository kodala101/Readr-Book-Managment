package bookapp.controllers.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for creating or modifying a personal reading journal entry.
 */
public record JournalEntryRequestDTO(
        @NotBlank(message = "Title is required.")
        @Size(max = 255, message = "Title cannot exceed 255 characters.")
        String title,

        @NotBlank(message = "Journal content is required.")
        @Size(max = 10000, message = "Content cannot exceed 10,000 characters.")
        String content,

        @Min(value = 1, message = "Page number must be a positive integer.")
        Integer pageNumber,

        @NotNull(message = "Book ID is required.")
        Long bookId
) {}