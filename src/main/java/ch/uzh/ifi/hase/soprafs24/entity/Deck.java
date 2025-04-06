package ch.uzh.ifi.hase.soprafs24.entity;
import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
@Entity
@Table(name = "deck")
public class Deck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = true)
    @JsonIgnore
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "invitation_id", nullable = true)
    @JsonIgnore
    private Invitation invitation;

    private String title;

    @Column(nullable = false)
    private FlashcardCategory deckCategory;

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Flashcard> flashcards = new ArrayList<>();  // Always initialized

    @Column(nullable = false)
    private Boolean isPublic;

    @Column
    private Boolean isAiGenerated;

    @Column(length = 2048) // Optional, allows longer prompts
    private String aiPrompt;

    // Transient field: not stored in database, defaults to null.
    @Transient
    private Integer numberOfAICards;

}