package bookapp.controllers.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for saving a highlighted book quote.
 */
public record QuoteRequestDTO(
        @NotBlank(message = "Quote text is required.")
        @Size(max = 3000, message = "Quote text cannot exceed 3,000 characters.")
        String text,

        @Min(value = 1, message = "Page number must be a positive integer.")
        Integer pageNumber,

        @NotNull(message = "Book ID is required.")
        Long bookId
) {}