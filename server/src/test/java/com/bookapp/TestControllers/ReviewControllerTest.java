package com.bookapp.TestControllers;

import bookapp.BookappApplication;
import bookapp.controllers.ReviewController;
import bookapp.controllers.dto.ReviewRequestDTO;
import bookapp.entities.Book;
import bookapp.entities.Review;
import bookapp.entities.User;
import bookapp.repositories.BookRepository;
import bookapp.repositories.ReviewRepository;
import bookapp.repositories.UserRepository;
import bookapp.security.service.AppUserDetails;
import bookapp.security.service.AppUserDetailsService;
import bookapp.security.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@ContextConfiguration(classes = BookappApplication.class)
@AutoConfigureMockMvc
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @MockBean
    private JwtService jwtService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    // --- Controller Repositories ---
    @MockBean
    private ReviewRepository reviewRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BookRepository bookRepository;

    private User mockUser;
    private Book mockBook;
    private Review mockReview;
    private AppUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .username("johndoe")
                .email("john@example.com")
                .build();

        mockBook = Book.builder()
                .id(10L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .build();

        mockReview = Review.builder()
                .id(100L)
                .rating(4.5)
                .content("Great book!")
                .createdAt(LocalDateTime.now())
                .user(mockUser)
                .book(mockBook)
                .build();

        mockUserDetails = new AppUserDetails(mockUser);
    }

    @Test
    @DisplayName("GET /api/reviews/book/{bookId} - Should return list of reviews")
    @WithMockUser
    void getReviewsByBook_ShouldReturnReviews() throws Exception {
        when(reviewRepository.findByBookId(10L)).thenReturn(List.of(mockReview));

        mockMvc.perform(get("/api/reviews/book/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].rating").value(4.5))
                .andExpect(jsonPath("$[0].content").value("Great book!"));

        verify(reviewRepository, times(1)).findByBookId(10L);
    }

    @Test
    @DisplayName("POST /api/reviews - Success when valid request")
    void createReview_ValidPayload_ShouldReturn201() throws Exception {
        ReviewRequestDTO request = new ReviewRequestDTO(4.5, "Awesome read", 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));
        when(reviewRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);
        when(reviewRepository.countByBookId(10L)).thenReturn(1);
        when(reviewRepository.findAverageRatingByBookId(10L)).thenReturn(Optional.of(4.5));

        mockMvc.perform(post("/api/reviews")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.rating").value(4.5));

        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("POST /api/reviews - Fails validation when rating exceeds 5.0")
    void createReview_InvalidRating_ShouldReturn400() throws Exception {
        ReviewRequestDTO request = new ReviewRequestDTO(6.0, "Invalid rating!", 10L);

        mockMvc.perform(post("/api/reviews")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{id} - Deletes review successfully")
    void deleteReview_OwnerUser_ShouldReturn204NoContent() throws Exception {
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));

        mockMvc.perform(delete("/api/reviews/100")
                        .with(user(mockUserDetails))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(reviewRepository, times(1)).delete(mockReview);
    }

    @Test
    @DisplayName("GET /api/reviews/me - Success returns current user reviews")
    void getCurrentUserReviews_ShouldReturnUserReviews() throws Exception {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.findByUser(mockUser)).thenReturn(List.of(mockReview));

        // Act & Assert
        mockMvc.perform(get("/api/reviews/me")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].content").value("Great book!"))
                .andExpect(jsonPath("$[0].rating").value(4.5));

        verify(userRepository, times(1)).findByUsername("johndoe");
        verify(reviewRepository, times(1)).findByUser(mockUser);
    }

    @Test
    @DisplayName("GET /api/reviews/me - Success returns empty list when user has no reviews")
    void getCurrentUserReviews_NoReviews_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.findByUser(mockUser)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/reviews/me")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));

        verify(reviewRepository, times(1)).findByUser(mockUser);
    }

    @Test
    @DisplayName("PUT /api/reviews/{id} - Success updates review when owned by current user")
    void updateReview_OwnerUser_ShouldReturnUpdatedReview() throws Exception {
        // Arrange
        ReviewRequestDTO updateRequest = new ReviewRequestDTO(5.0, "Updated content!", 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(mockReview));

        when(bookRepository.findById(10L)).thenReturn(Optional.of(mockBook));

        when(reviewRepository.save(any(Review.class))).thenReturn(mockReview);
        when(reviewRepository.countByBookId(10L)).thenReturn(1);
        when(reviewRepository.findAverageRatingByBookId(10L)).thenReturn(Optional.of(5.0));

        mockMvc.perform(put("/api/reviews/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100));

        verify(reviewRepository, times(1)).save(mockReview);
        verify(bookRepository, times(1)).findById(10L); // Optional verification
    }

    @Test
    @DisplayName("PUT /api/reviews/{id} - Throws Exception when user is not the owner")
    void updateReview_NotOwner_ShouldFail() throws Exception {
        // Arrange
        User otherUser = User.builder().id(99L).username("otheruser").build();
        Review reviewOwnedByOther = Review.builder()
                .id(100L)
                .user(otherUser) // Owned by user ID 99
                .book(mockBook)
                .build();

        ReviewRequestDTO updateRequest = new ReviewRequestDTO(5.0, "Hacking content", 10L);

        // Mock currentUser as ID 1 (mockUser), but review belongs to ID 99 (otherUser)
        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(reviewOwnedByOther));

        // Act & Assert
        mockMvc.perform(put("/api/reviews/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertEquals(
                                "You are not allowed to update this review.",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/reviews/{id} - Throws Exception when review does not exist")
    void updateReview_NotFound_ShouldFail() throws Exception {
        // Arrange
        ReviewRequestDTO updateRequest = new ReviewRequestDTO(5.0, "Updated content!", 10L);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(mockUser));
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(put("/api/reviews/999")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(result ->
                        Assertions.assertInstanceOf(IllegalArgumentException.class, result.getResolvedException())
                )
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertEquals(
                                "Review not found with id: 999",
                                Objects.requireNonNull(result.getResolvedException()).getMessage()
                        )
                );

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/reviews/{id} - Fails validation on invalid payload rating")
    void updateReview_InvalidRating_ShouldReturn400() throws Exception {
        // Arrange - rating higher than allowed max 5.0
        ReviewRequestDTO invalidRequest = new ReviewRequestDTO(10.0, "Way too high!", 10L);

        // Act & Assert
        mockMvc.perform(put("/api/reviews/100")
                        .with(user(mockUserDetails))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewRepository, never()).save(any());
    }
}