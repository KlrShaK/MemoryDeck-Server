package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.Statistics;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.StatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {

    @Mock
    private StatisticsRepository statisticsRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void recordQuizStats_createsNewStatistics_whenNoneExists() {
        User user = new User();
        user.setId(1L);
        Quiz quiz = new Quiz();
        quiz.setId(2L);

        int score = 42;
        int attempts = 3;
        long timeTakenMillis = 2500L;  // will map to timeTaken = 2

        when(statisticsRepository
                .lockByQuizIdAndUserId(quiz.getId(), user.getId()))
                .thenReturn(Optional.empty());

        ArgumentCaptor<Statistics> captor = ArgumentCaptor.forClass(Statistics.class);

        statisticsService.recordQuizStats(user, quiz, score, attempts, timeTakenMillis);

        verify(statisticsRepository).save(captor.capture());
        Statistics saved = captor.getValue();

        assertNotNull(saved);
        assertEquals(user, saved.getUser());
        assertEquals(quiz, saved.getQuiz());
        assertEquals(score, saved.getScore());
        assertEquals(attempts, saved.getNumberOfAttempts());
        assertEquals(timeTakenMillis / 1000L, saved.getTimeTaken());
        assertNotNull(saved.getQuizDate());
    }

    @Test
    void recordQuizStats_updatesExistingStatistics_whenExists() {
        User user = new User();
        user.setId(5L);
        Quiz quiz = new Quiz();
        quiz.setId(7L);

        Statistics existing = new Statistics();
        existing.setUser(user);
        existing.setQuiz(quiz);
        existing.setScore(10);
        existing.setNumberOfAttempts(1);
        existing.setTimeTaken(1L);               // now using a Long literal
        Date oldDate = new Date(0L);
        existing.setQuizDate(oldDate);

        when(statisticsRepository
                .lockByQuizIdAndUserId(quiz.getId(), user.getId()))
                .thenReturn(Optional.of(existing));

        int newScore = 99;
        int newAttempts = 4;
        long newTimeMillis = 4200L;             // will map to timeTaken = 4

        ArgumentCaptor<Statistics> captor = ArgumentCaptor.forClass(Statistics.class);

        statisticsService.recordQuizStats(user, quiz, newScore, newAttempts, newTimeMillis);

        verify(statisticsRepository).save(captor.capture());
        Statistics updated = captor.getValue();

        assertSame(existing, updated);
        assertEquals(newScore, updated.getScore());
        assertEquals(newAttempts, updated.getNumberOfAttempts());
        assertEquals(newTimeMillis / 1000L, updated.getTimeTaken());
        assertNotNull(updated.getQuizDate());
        assertTrue(updated.getQuizDate().getTime() > oldDate.getTime());
    }
}
