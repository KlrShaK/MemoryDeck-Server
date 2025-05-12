package ch.uzh.ifi.hase.soprafs24.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"quiz_id", "user_id"}))
public class Statistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user for whom these statistics are recorded.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Optional link to the quiz session this record relates to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // Final score achieved
    @Column(nullable = false)
    private int score;

    // Total time taken for the quiz (e.g., in seconds)
    @Column(nullable = false)
    private Long timeTaken;

    // Total number of attempts across all questions during the quiz
    @Column(nullable = false)
    private int numberOfAttempts;

    // Date when the quiz was completed
    @Column(nullable = false)
    private Date quizDate;

    /** `true` if this row belongs to the quiz winner, `false`   *
     *  if we explicitly mark the loser, `null` = draw / unknown */
    @Column(name = "IS_WINNER")
    private Boolean isWinner;
}
