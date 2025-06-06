package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.QuizStatus;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.QuizRepository;
import ch.uzh.ifi.hase.soprafs24.repository.ScoreRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.QuizDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class QuizMapperTest {

    private ScoreRepository scoreRepository;
    private QuizRepository quizRepository;
    private DeckRepository deckRepository;
    private QuizMapper quizMapper;
    private ScoreMapper  scoreMapper;

    @BeforeEach
    public void setup() {
        scoreRepository = mock(ScoreRepository.class);
        quizRepository = mock(QuizRepository.class);
        deckRepository = mock(DeckRepository.class);
        scoreMapper = mock(ScoreMapper.class);
        quizMapper = new QuizMapper(scoreRepository, quizRepository, deckRepository,scoreMapper);
    }

    @Test
    public void testToDTO_mapsCorrectly() {
        // Setup
        Quiz quiz = new Quiz();
        quiz.setId(1L);
        quiz.setTimeLimit(300);
        quiz.setStartTime(new Date());
        quiz.setEndTime(new Date());
        quiz.setQuizStatus(QuizStatus.IN_PROGRESS);
        quiz.setIsMultiple(true);

        Deck deck = new Deck();
        deck.setId(100L);
        quiz.setDecks(List.of(deck));

        Score score = new Score();
        score.setId(1L);
        score.setCorrectQuestions(7);
        score.setTotalQuestions(10);
        score.setQuiz(quiz);
        quiz.setScores(List.of(score));

        Invitation invitation = new Invitation();
        quiz.setInvitation(invitation);

        User winner = new User();
        winner.setId(999L);
        quiz.setWinner(winner.getId());

        // Act
        QuizDTO dto = quizMapper.toDTO(quiz);

        // Assert
        assertEquals(quiz.getId(), dto.getId());
        assertEquals(quiz.getTimeLimit(), dto.getTimeLimit());
        assertEquals(quiz.getQuizStatus().toString(), dto.getQuizStatus());
        assertEquals(quiz.getDecks(), dto.getDecks());
        assertEquals(scoreMapper.toDTOList(quiz.getScores()), dto.getScores());
        assertEquals(quiz.getInvitation().getId(), dto.getInvitationId());
        // assertEquals(quiz.getWinner(), dto.getWinner());
        assertTrue(dto.getIsMultiple());
    }

    @Test
    public void testToDTOList_mapsListCorrectly() {
        Quiz quiz = new Quiz();
        quiz.setId(1L);
        quiz.setQuizStatus(QuizStatus.COMPLETED);
        List<QuizDTO> result = quizMapper.toDTOList(List.of(quiz));
        assertEquals(1, result.size());
        assertEquals(quiz.getId(), result.get(0).getId());
    }

    // @Test
    // public void testToEntity_mapsCorrectly() {
    //     QuizDTO dto = new QuizDTO();
    //     dto.setId(10L);
    //     dto.setTimeLimit(200);
    //     dto.setStartTime(new Date());
    //     dto.setEndTime(new Date());
    //     dto.setQuizStatus((QuizStatus.WAITING).toString());
    //     dto.setIsMultiple(false);

    //     Deck deck = new Deck();
    //     dto.setDecks(List.of(deck));
    //     Score score = new Score();
    //     dto.setScores(scoreMapper.toDTOList(List.of(score)));
    //     Invitation invitation = new Invitation();
    //     dto.setInvitation(invitation);
        // User winner = new User();
        // dto.setWinner(winner.getId());

        // Quiz quiz = quizMapper.toEntity(dto);
        // assertEquals(dto.getId(), quiz.getId());
        // assertEquals(dto.getTimeLimit(), quiz.getTimeLimit());
        // assertEquals(dto.getQuizStatus(), quiz.getQuizStatus());
        // assertEquals(dto.getDecks(), quiz.getDecks());
        // assertEquals(dto.getScores(), quiz.getScores());
        // assertEquals(dto.getInvitation(), quiz.getInvitation());
        // assertEquals(dto.getWinner(), quiz.getWinner());
        // assertFalse(quiz.getIsMultiple());
    // }

    @Test
    public void testFromInvitationToEntity_createsAndSavesQuizAndScores() {
        // Arrange
        Invitation invitation = new Invitation();
        invitation.setTimeLimit(120);
        invitation.setDecks(new ArrayList<>());
        Deck deck = new Deck();
        deck.setId(1L);
        Flashcard flashcard = new Flashcard();
        flashcard.setId(1L);
        flashcard.setDescription("temp");
        flashcard.setDeck(deck);
        deck.setFlashcards(Arrays.asList(flashcard));
        invitation.getDecks().add(deck);

        User fromUser = new User();
        fromUser.setId(1L);
        User toUser = new User();
        toUser.setId(2L);
        invitation.setFromUser(fromUser);
        invitation.setToUser(toUser);

        when(deckRepository.findById(deck.getId())).thenReturn(Optional.of(deck));
        when(quizRepository.save(any(Quiz.class))).thenAnswer(inv -> inv.getArgument(0));
        when(scoreRepository.save(any(Score.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Quiz quiz = quizMapper.fromInvitationToEntity(invitation);

        // Assert
        assertNotNull(quiz);
        assertEquals(QuizStatus.WAITING, quiz.getQuizStatus());
        assertEquals(1, quiz.getDecks().size());
        assertEquals(deck.getId(), quiz.getDecks().get(0).getId());
        assertEquals(2, quiz.getScores().size());
        verify(scoreRepository, times(2)).save(any(Score.class));
        verify(quizRepository, atLeastOnce()).save(any(Quiz.class));
    }
}
