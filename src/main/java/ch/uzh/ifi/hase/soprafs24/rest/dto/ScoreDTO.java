package ch.uzh.ifi.hase.soprafs24.rest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter // Generates getters, setters automatically
public class ScoreDTO {

    private Long id;

    private Long quizId; 

    private Long userId; 

    private int correctQuestions;

    private int totalQuestions;
}
