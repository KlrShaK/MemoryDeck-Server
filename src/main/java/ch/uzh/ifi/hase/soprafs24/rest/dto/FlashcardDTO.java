package ch.uzh.ifi.hase.soprafs24.rest.dto;


import java.time.LocalDate;

import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class FlashcardDTO {

    private Long id;

    private Deck deck;

    private String imageUrl;

    private String description;

    private LocalDate date;

    private String answer;

    private FlashcardCategory flashcardCategory;

    private String[] wrongAnswers;

}