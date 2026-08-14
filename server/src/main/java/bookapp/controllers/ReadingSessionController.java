package bookapp.controllers;

import bookapp.controllers.dto.ReadingSessionRequestDTO;
import bookapp.controllers.dto.ReadingSessionResponseDTO;
import bookapp.entities.Book;
import bookapp.entities.ReadingSession;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.ReadingSessionRepository;
import bookapp.repositories.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for recording active reading timers and page logs.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class ReadingSessionController {

    private final ReadingSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    /**
     * Retrieves all reading sessions recorded by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<ReadingSessionResponseDTO>> getUserSessions(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<ReadingSessionResponseDTO> sessions = sessionRepository.findByUserIdOrderBySessionDateDesc(user.getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieves all sessions for a specific book.
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<List<ReadingSessionResponseDTO>> getSessionsByBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        List<ReadingSessionResponseDTO> sessions = sessionRepository.findByUserIdAndBookId(user.getId(), bookId)
                .stream()
                .map(this::toResponseDTO)
                .toList();

        return ResponseEntity.ok(sessions);
    }

    /**
     * Retrieves the latest reading session for the authenticated user.
     */
    @GetMapping("/latest")
    public ResponseEntity<ReadingSessionResponseDTO> getLatestSession(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        ReadingSession latestSession = sessionRepository.findFirstByUserIdOrderBySessionDateDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No reading sessions found."));

        return ResponseEntity.ok(toResponseDTO(latestSession));
    }

    /**
     * Gets total reading minutes since the given date.
     */
    @GetMapping("/stats/minutes")
    public ResponseEntity<Integer> getTotalMinutesSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        return ResponseEntity.ok(sessionRepository.getTotalMinutesReadSince(user.getId(), startDate));
    }

    /**
     * Gets total pages read for a specific book.
     */
    @GetMapping("/stats/books/{bookId}/pages")
    public ResponseEntity<Integer> getTotalPagesForBook(
            @PathVariable Long bookId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        return ResponseEntity.ok(sessionRepository.getTotalPagesReadForBook(user.getId(), bookId));
    }

    /**
     * Logs a completed reading session.
     */
    @PostMapping
    public ResponseEntity<ReadingSessionResponseDTO> logSession(
            @Valid @RequestBody ReadingSessionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        validatePageRange(request.startPage(), request.endPage());

        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + request.bookId()));

        ReadingSession session = ReadingSession.builder()
                .minutesRead(request.minutesRead())
                .startPage(request.startPage())
                .endPage(request.endPage())
                .sessionDate(request.sessionDate() != null ? request.sessionDate() : LocalDateTime.now())
                .user(user)
                .book(book)
                .build();

        ReadingSession savedSession = sessionRepository.save(session);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedSession));
    }

    /**
     * Updates a reading session owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReadingSessionResponseDTO> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody ReadingSessionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        ReadingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reading session not found with id: " + id));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to update this session.");
        }

        validatePageRange(request.startPage(), request.endPage());

        session.setMinutesRead(request.minutesRead());
        session.setStartPage(request.startPage());
        session.setEndPage(request.endPage());
        session.setSessionDate(request.sessionDate() != null ? request.sessionDate() : session.getSessionDate());

        ReadingSession updatedSession = sessionRepository.save(session);

        return ResponseEntity.ok(toResponseDTO(updatedSession));
    }

    /**
     * Deletes a reading session owned by the authenticated user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthenticatedUser(userDetails);

        ReadingSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reading session not found with id: " + id));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You are not allowed to delete this session.");
        }

        sessionRepository.delete(session);

        return ResponseEntity.noContent().build();
    }

    private void validatePageRange(Integer startPage, Integer endPage) {
        if (startPage != null && endPage != null && endPage < startPage) {
            throw new IllegalArgumentException("End page cannot be less than start page.");
        }
    }

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private ReadingSessionResponseDTO toResponseDTO(ReadingSession session) {
        return new ReadingSessionResponseDTO(
                session.getId(),
                session.getMinutesRead(),
                session.getStartPage(),
                session.getEndPage(),
                session.getSessionDate(),
                session.getUser().getId(),
                session.getBook().getId(),
                session.getBook().getTitle()
        );
    }
}