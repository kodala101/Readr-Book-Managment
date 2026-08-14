package bookapp.controllers.dto;

public record QuoteResponseDTO(
        Long id,
        String quoteText,
        Integer pageNumber,
        Long userId,
        Long bookId,
        String bookTitle
) {}
