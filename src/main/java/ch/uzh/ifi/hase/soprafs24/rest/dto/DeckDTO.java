package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;


import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.entity.Invitation;
import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class DeckDTO {

    private Long id;

    private User user;

    private String title;

    private FlashcardCategory deckCategory;

    private List<Flashcard> flashcards; 

    private Quiz quiz;

    private Invitation invitation;

    private Boolean isPublic;

    private Boolean isAiGenerated;

    private String aiPrompt;

    private Integer numberOfAICards;

}