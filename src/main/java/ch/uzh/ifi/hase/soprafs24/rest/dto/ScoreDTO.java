package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class ScoreDTO {

    private Long id;

    private Quiz quiz; 

    private User user; 

    private int correctQuestions;

    private int totalQuestions;
}
