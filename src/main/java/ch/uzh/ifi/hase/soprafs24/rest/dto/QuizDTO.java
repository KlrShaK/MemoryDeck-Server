package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Date;
import java.util.List;

import ch.uzh.ifi.hase.soprafs24.constant.QuizStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Invitation;
import ch.uzh.ifi.hase.soprafs24.entity.Score;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class QuizDTO {

    private Long id;

    private List<Deck> decks;

    private List<Score> scores;

    private Invitation invitation; 

    private Date startTime;
 
    private Date endTime;

    private int timeLimit;

    private QuizStatus quizStatus;

    private Long winner;

    private Boolean isMultiple;

}
