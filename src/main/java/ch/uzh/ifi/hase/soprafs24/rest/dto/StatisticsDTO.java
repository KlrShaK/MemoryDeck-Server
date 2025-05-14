package ch.uzh.ifi.hase.soprafs24.rest.dto;

import java.util.Date;

import lombok.Data;

@Data
public class StatisticsDTO {
    private Long   userId;
    private String username;
    private Long   quizId;
    private int    correctQuestions;   // a.k.a. “score”
    private int    numberOfAttempts;
    private long   timeTakenSeconds;
    private Date   quizDate;
    private Boolean isWinner;
}
