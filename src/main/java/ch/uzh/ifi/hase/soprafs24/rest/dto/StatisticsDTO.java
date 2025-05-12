package ch.uzh.ifi.hase.soprafs24.rest.dto;

import lombok.Data;

@Data
public class StatisticsDTO {
    private Long   userId;
    private String username;
    private Long   quizId;
    private int    correctQuestions;   // a.k.a. “score”
    private int    numberOfAttempts;
    private long   timeTakenSeconds;
    private Boolean isWinner;
}
