package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.List;

import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class InvitationDTO {

    private Long id;

    private List<Deck> decks;

    private Long fromUserId;

    private Long toUserId;

    private Long quizId;

    private int timeLimit;

    private Boolean isAccepted;

}
