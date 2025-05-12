// Quiz.java
package ch.uzh.ifi.hase.soprafs24.entity;

import java.util.Date;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.uzh.ifi.hase.soprafs24.constant.QuizStatus;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
@Entity
@Table(name = "quiz")
public class Quiz implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // If multiple decks can be used, you already have getDecks().
    // To store the *actual questions*, let's define a ManyToMany of Flashcards.
    @ManyToMany
    @JoinTable(
            name = "quiz_flashcards",
            joinColumns = @JoinColumn(name = "quiz_id"),
            inverseJoinColumns = @JoinColumn(name = "flashcard_id")
    )
    private List<Flashcard> selectedFlashcards = new ArrayList<>();

    // Associated decks (for example, if multiple decks/questions are used)
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Deck> decks = new ArrayList<>();

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Score> scores = new ArrayList<>();

    @OneToOne(mappedBy = "quiz")
    @JsonIgnore
    private Invitation invitation;

    @Column(nullable = false)
    private Date startTime;
 
    @Column(nullable = true)
    private Date endTime;

    @Column(nullable = false)
    private int timeLimit;

    @Column(nullable = false)
    private QuizStatus quizStatus;

    @Column(nullable = true)
    private Long winner;

    @Column(nullable = false)
    private Boolean isMultiple;

}
