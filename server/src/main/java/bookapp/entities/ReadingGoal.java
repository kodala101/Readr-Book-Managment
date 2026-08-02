package bookapp.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reading_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer targetYear;
    private Integer targetBooksCount;
    private Integer targetPagesCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
