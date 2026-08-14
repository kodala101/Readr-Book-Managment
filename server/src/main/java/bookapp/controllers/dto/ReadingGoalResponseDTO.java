package bookapp.controllers.dto;

public record ReadingGoalResponseDTO(
        Long id,
        Integer targetYear,
        Integer targetBooksCount,
        Integer targetPagesCount,
        Long userId
) {}
