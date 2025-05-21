package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.Score;
import ch.uzh.ifi.hase.soprafs24.entity.Statistics;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.InvitationRepository;
import ch.uzh.ifi.hase.soprafs24.repository.QuizRepository;
import ch.uzh.ifi.hase.soprafs24.repository.ScoreRepository;
import ch.uzh.ifi.hase.soprafs24.repository.StatisticsRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.QuizAnswerResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.FlashcardMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.QuizMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = QuizServiceIntegrationTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@AutoConfigureTestDatabase   // brings up an embedded H2
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class QuizServiceIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan("ch.uzh.ifi.hase.soprafs24.entity")
    @EnableJpaRepositories("ch.uzh.ifi.hase.soprafs24.repository")
    @Import({ QuizService.class, StatisticsService.class })
    static class TestConfig {}

    @Autowired private QuizService           quizService;
    @Autowired private StatisticsService     statisticsService;
    @Autowired private UserRepository        userRepository;
    @Autowired private DeckRepository        deckRepository;
    @Autowired private QuizRepository        quizRepository;
    @Autowired private ScoreRepository       scoreRepository;
    @Autowired private StatisticsRepository  statisticsRepository;

    @MockBean private QuizMapper             quizMapper;
    @MockBean private FlashcardMapper        flashcardMapper;
    @MockBean private SimpMessagingTemplate  messagingTemplate;
    @MockBean private UserService            userService;
    @MockBean private InvitationRepository   invitationRepository;

    private User user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        // 1) Persist a fully valid User (populate all non-nullable columns)
        user = new User();
        user.setName("alice");
        user.setUsername("alice123");          // nullable=false, unique=true
        user.setPassword("secret");            // nullable=false
        user.setStatus(UserStatus.OFFLINE);    // nullable=false
        user.setCreationDate(new Date());      // nullable=false
        // birthday and token may remain null
        user = userRepository.saveAndFlush(user);

        // 2) Persist a Deck with two Flashcards
        deck = new Deck();
        deck.setTitle("Integration Deck");
        deck.setUser(user);
        deck.setDeckCategory(FlashcardCategory.OTHERS);
        deck.setIsPublic(true);

        Flashcard c1 = new Flashcard();
        c1.setDescription("Q1?");
        c1.setAnswer("A1");
        c1.setWrongAnswers(new String[]{"X","Y","Z"});
        c1.setDeck(deck);

        Flashcard c2 = new Flashcard();
        c2.setDescription("Q2?");
        c2.setAnswer("A2");
        c2.setWrongAnswers(new String[]{"X","Y","Z"});
        c2.setDeck(deck);

        deck.getFlashcards().addAll(List.of(c1, c2));
        deck = deckRepository.saveAndFlush(deck);

        // 3) Stub out invitation-based mapping (not used by startQuiz)
        Mockito.when(quizMapper.fromInvitationToEntity(Mockito.any()))
                .thenThrow(new IllegalStateException("Shouldn’t be called in this test"));
    }

    @Test
    void fullSoloQuizFlow_updatesScoreAndStatistics_andResetsUserStatus() {
        // Start a 2-question solo quiz
        Quiz quiz = quizService.startQuiz(deck.getId(), 2, null, false);
        assertThat(quiz.getSelectedFlashcards()).hasSize(2);

        // Answer each question correctly
        var cards = quiz.getSelectedFlashcards();
        for (int i = 0; i < cards.size(); i++) {
            QuizAnswerResponseDTO dto = quizService.processAnswerWithFeedback(
                    quiz.getId(),
                    cards.get(i).getId(),
                    cards.get(i).getAnswer(),
                    user.getId()
            );
            if (i < cards.size() - 1) {
                assertThat(dto.isFinished()).isFalse();
            } else {
                assertThat(dto.isFinished()).isTrue();
            }
            assertThat(dto.isWasCorrect()).isTrue();
        }

        // Verify a single Score row exists
        Score score = scoreRepository.findByQuizIdAndUserId(quiz.getId(), user.getId());
        assertThat(score).isNotNull();
        assertThat(score.getCorrectQuestions()).isEqualTo(2);
        assertThat(score.getTotalQuestions()).isEqualTo(2);

        // Verify a single Statistics row exists and has aggregated both attempts
        List<Statistics> stats = statisticsRepository.findByQuiz_Id(quiz.getId());
        assertThat(stats).hasSize(1);
        Statistics stat = stats.get(0);
        assertThat(stat.getScore()).isEqualTo(2);
        assertThat(stat.getNumberOfAttempts()).isEqualTo(2);
        assertThat(stat.getTimeTaken()).isGreaterThanOrEqualTo(0L);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserStatus.PLAYING);
    }
}
