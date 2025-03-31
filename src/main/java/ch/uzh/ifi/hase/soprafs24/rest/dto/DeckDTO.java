package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;


import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.entity.User;

public class DeckDTO {

    private Long id;

    private User user;

    private String title;

    private FlashcardCategory deckCategory;

    private List<Flashcard> flashcards; 

    private Boolean isPublic;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FlashcardCategory getDeckCategory() {
        return deckCategory;
    }

    public void setDeckCategory(FlashcardCategory deckCategory) {
        this.deckCategory = deckCategory;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<Flashcard> getFlashcards() {
        return flashcards;
      }
    
    public void setFlashcards(List<Flashcard> flashcards) {
        this.flashcards = flashcards;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


}
