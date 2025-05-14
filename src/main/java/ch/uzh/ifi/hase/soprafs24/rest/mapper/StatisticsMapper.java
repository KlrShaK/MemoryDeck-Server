package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Statistics;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StatisticsDTO;
import org.springframework.stereotype.Component;

@Component
public class StatisticsMapper {

    public StatisticsDTO toDTO(Statistics s) {
        StatisticsDTO dto = new StatisticsDTO();
        dto.setQuizId(s.getQuiz().getId());
        dto.setUserId(s.getUser().getId());
        dto.setUsername(s.getUser().getUsername());
        dto.setCorrectQuestions(s.getScore());
        dto.setNumberOfAttempts(s.getNumberOfAttempts());
        dto.setTimeTakenSeconds(s.getTimeTaken());
        dto.setQuizDate(s.getQuizDate());
        dto.setIsWinner(s.getIsWinner());
        return dto;
    }
}
