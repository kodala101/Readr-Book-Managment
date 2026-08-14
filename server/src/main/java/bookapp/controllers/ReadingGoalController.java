package bookapp.controllers;

import bookapp.controllers.dto.ReadingGoalRequestDTO;
import bookapp.controllers.dto.ReadingGoalResponseDTO;
import bookapp.entities.ReadingGoal;
import bookapp.entities.User;
import bookapp.repositories.ReadingGoalRepository;
import bookapp.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for creating and tracking reading goals.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/goals")
public class ReadingGoalController {

    private final ReadingGoalRepository goalRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves all reading goals for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ReadingGoalResponseDTO>> getUserGoals(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<ReadingGoalResponseDTO> goals = goalRepository.findByUser(user)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(goals);
    }

    /**
     * Retrieves a reading goal for a specific year.
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<ReadingGoalResponseDTO> getGoalByYear(
            @PathVariable Integer year,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        ReadingGoal goal = goalRepository.findByUserIdAndTargetYear(user.getId(), year)
                .orElseThrow(() -> new IllegalArgumentException("Reading goal not found for year: " + year));

        return ResponseEntity.ok(toResponseDTO(goal));
    }

    /**
     * Creates a new reading goal.
     */
    @PostMapping
    public ResponseEntity<ReadingGoalResponseDTO> createGoal(
            @Valid @RequestBody ReadingGoalRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        goalRepository.findByUserIdAndTargetYear(user.getId(), request.targetYear())
                .ifPresent(existingGoal -> {
                    throw new IllegalArgumentException("Reading goal already exists for year: " + request.targetYear());
                });

        ReadingGoal goal = ReadingGoal.builder()
                .targetYear(request.targetYear())
                .targetBooksCount(request.targetBooks())
                .targetPagesCount(request.targetPagesCount())
                .user(user)
                .build();

        ReadingGoal savedGoal = goalRepository.save(goal);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedGoal));
    }

    /**
     * Updates an existing reading goal.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReadingGoalResponseDTO> updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody ReadingGoalRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        ReadingGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reading goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to update this goal.");
        }

        // Prevent updating the targetYear to a year that already exists on another goal for this user
        if (!goal.getTargetYear().equals(request.targetYear())) {
            goalRepository.findByUserIdAndTargetYear(user.getId(), request.targetYear())
                    .ifPresent(existingGoal -> {
                        throw new IllegalArgumentException("Reading goal already exists for year: " + request.targetYear());
                    });
        }

        goal.setTargetYear(request.targetYear());
        goal.setTargetBooksCount(request.targetBooks());
        goal.setTargetPagesCount(request.targetPagesCount());

        ReadingGoal updatedGoal = goalRepository.save(goal);

        return ResponseEntity.ok(toResponseDTO(updatedGoal));
    }

    /**
     * Deletes a reading goal owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        ReadingGoal goal = goalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reading goal not found with id: " + id));

        if (!goal.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to delete this goal.");
        }

        goalRepository.delete(goal);

        return ResponseEntity.noContent().build();
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private ReadingGoalResponseDTO toResponseDTO(ReadingGoal goal) {
        return new ReadingGoalResponseDTO(
                goal.getId(),
                goal.getTargetYear(),
                goal.getTargetBooksCount(),
                goal.getTargetPagesCount(),
                goal.getUser().getId()
        );
    }
}