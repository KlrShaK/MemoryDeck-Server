package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.Score;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.ScoreDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreMapperTest {

    private ScoreMapper scoreMapper;

    @BeforeEach
    public void setup() {
        scoreMapper = new ScoreMapper();
    }

    @Test
    void testToDTO() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");

        Quiz quiz = new Quiz();
        quiz.setId(10L);

        Score score = new Score();
        score.setId(100L);
        score.setUser(user);
        score.setQuiz(quiz);
        score.setCorrectQuestions(5);
        score.setTotalQuestions(10);

        // Act
        ScoreDTO dto = scoreMapper.toDTO(score);

        // Assert
        assertNotNull(dto);
        assertEquals(score.getId(), dto.getId());
        assertEquals(score.getUser().getId(), dto.getUserId());
        assertEquals(score.getQuiz().getId(), dto.getQuizId());
        assertEquals(score.getCorrectQuestions(), dto.getCorrectQuestions());
        assertEquals(score.getTotalQuestions(), dto.getTotalQuestions());
    }

    @Test
    void testToDTOList() {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("testUser");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("testUser1");

        Quiz quiz = new Quiz();
        quiz.setId(1L);

        Score score1 = new Score();
        score1.setId(1L);
        score1.setCorrectQuestions(3);
        score1.setTotalQuestions(5);
        score1.setUser(user1);
        score1.setQuiz(quiz);

        Score score2 = new Score();
        score2.setId(2L);
        score2.setCorrectQuestions(7);
        score2.setTotalQuestions(10);
        score2.setUser(user2);
        score2.setQuiz(quiz);

        List<Score> scores = Arrays.asList(score1, score2);
        quiz.setScores(scores);

        // Act
        List<ScoreDTO> dtoList = scoreMapper.toDTOList(scores);

        // Assert
        assertNotNull(dtoList);
        assertEquals(2, dtoList.size());
        assertEquals(3, dtoList.get(0).getCorrectQuestions());
        assertEquals(7, dtoList.get(1).getCorrectQuestions());
    }

    // @Test
    // void testToEntity() {
    //     // Arrange
    //     User user = new User();
    //     user.setId(2L);

    //     Quiz quiz = new Quiz();
    //     quiz.setId(22L);

    //     ScoreDTO dto = new ScoreDTO();
    //     dto.setId(200L);
    //     dto.setUserId(user.getId());
    //     dto.setQuizId(quiz.getId());
    //     dto.setCorrectQuestions(8);
    //     dto.setTotalQuestions(12);

    //     // Act
    //     Score entity = scoreMapper.toEntity(dto);

    //     // Assert
    //     assertNotNull(entity);
    //     assertEquals(dto.getId(), entity.getId());
    //     assertEquals(dto.getUser(), entity.getUser());
    //     assertEquals(dto.getQuiz(), entity.getQuiz());
    //     assertEquals(dto.getCorrectQuestions(), entity.getCorrectQuestions());
    //     assertEquals(dto.getTotalQuestions(), entity.getTotalQuestions());
    // }
}
