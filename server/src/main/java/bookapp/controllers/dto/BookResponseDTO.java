package bookapp.controllers.dto;

public record BookResponseDTO(
        Long id,
        String title,
        String author,
        String isbn,
        String description,
        String coverImageUrl,
        Double averageRating,
        Integer totalRatings,
        Integer pageCount,
        Integer publicationYear
) {}
