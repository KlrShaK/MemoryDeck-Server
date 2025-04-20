package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.FlashcardCategory;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Invitation;
import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.InvitationRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InvitationDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;


@WebAppConfiguration
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class QuizServiceIntegrationTest {

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    private User user1;
    private User user2;
    private Deck testDeck;

    @Transactional
    @BeforeEach
    void setup() {
        // Create users
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("Alice");
        user1.setPassword("testPassword");
        user1.setCreationDate(new Date());
        user1.setStatus(UserStatus.ONLINE);
        userRepository.save(user1);

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("Bob");
        user2.setPassword("testPassword");
        user2.setCreationDate(new Date());
        user2.setStatus(UserStatus.ONLINE);
        userRepository.save(user2);
        userRepository.flush();

        // Create deck
        testDeck = new Deck();
        testDeck.setId(1L);
        testDeck.setTitle("Sample Deck");
        testDeck.setDeckCategory(FlashcardCategory.SCIENCE);
        testDeck.setIsPublic(true);
        testDeck.setUser(user1);
        deckRepository.save(testDeck);
        deckRepository.flush();
    }

    @Test
    void createInvitation_success() {
        InvitationDTO dto = new InvitationDTO();
        dto.setFromUserId(user1.getId());
        dto.setToUserId(user2.getId());
        dto.setTimeLimit(300);
        dto.setDeckIds(Collections.singletonList(testDeck.getId()));

        Invitation created = quizService.createInvitation(dto);

        assertNotNull(created.getId());
        assertEquals(user1.getId(), created.getFromUser().getId());
        assertEquals(user2.getId(), created.getToUser().getId());
        assertFalse(created.getIsAccepted());
        assertEquals(1, created.getDecks().size());
    }

    @Test
    void createQuiz_success() {
        // First create an invitation
        InvitationDTO dto = new InvitationDTO();
        dto.setFromUserId(user1.getId());
        dto.setToUserId(user2.getId());
        dto.setTimeLimit(300);
        dto.setDeckIds(Collections.singletonList(testDeck.getId()));
        Invitation invitation = quizService.createInvitation(dto);

        // Create quiz from invitation
        Quiz quiz = quizService.createQuiz(invitation.getId());

        assertNotNull(quiz.getId());
        assertEquals(300, quiz.getTimeLimit());
        assertNotNull(quiz.getInvitation());
    }

    @Test
    void confirmInvitation_success() {
        // Prepare data
        InvitationDTO dto = new InvitationDTO();
        dto.setFromUserId(user1.getId());
        dto.setToUserId(user2.getId());
        dto.setTimeLimit(300);
        dto.setDeckIds(Collections.singletonList(testDeck.getId()));
        Invitation invitation = quizService.createInvitation(dto);

        quizService.createQuiz(invitation.getId());

        // Act
        quizService.confirmedInvitation(invitation.getId());

        Invitation updated = quizService.getInvitationById(invitation.getId());

        assertTrue(updated.getIsAccepted());
        assertNotNull(updated.getIsAcceptedDate());
        assertEquals(UserStatus.PLAYING, updated.getFromUser().getStatus());
        assertEquals(UserStatus.PLAYING, updated.getToUser().getStatus());

        Quiz quiz = updated.getQuiz();
        assertNotNull(quiz.getStartTime());
        assertEquals(quiz.getQuizStatus().name(), "IN_PROGRESS");
    }

    @Test
    void rejectInvitation_success() {
        InvitationDTO dto = new InvitationDTO();
        dto.setFromUserId(user1.getId());
        dto.setToUserId(user2.getId());
        dto.setTimeLimit(300);
        dto.setDeckIds(Collections.singletonList(testDeck.getId()));
        Invitation invitation = quizService.createInvitation(dto);

        quizService.createQuiz(invitation.getId());

        quizService.rejectedInvitation(invitation.getId());

        assertFalse(invitationRepository.findById(invitation.getId()).isPresent());
    }

    @Test
    void findInvitationByFromUserIdAndIsAcceptedTrue_onlyKeepsEarliest() {
        // First accepted invitation
        InvitationDTO dto1 = new InvitationDTO();
        dto1.setFromUserId(user1.getId());
        dto1.setToUserId(user2.getId());
        dto1.setTimeLimit(300);
        dto1.setDeckIds(Collections.singletonList(testDeck.getId()));
        Invitation i1 = quizService.createInvitation(dto1);
        quizService.createQuiz(i1.getId());

        // Second (late) accepted invitation
        InvitationDTO dto2 = new InvitationDTO();
        dto2.setFromUserId(user1.getId());
        dto2.setToUserId(user2.getId());
        dto2.setTimeLimit(300);
        dto2.setDeckIds(Collections.singletonList(testDeck.getId()));
        Invitation i2 = quizService.createInvitation(dto2);
        quizService.createQuiz(i2.getId());

        quizService.confirmedInvitation(i1.getId());
        quizService.confirmedInvitation(i2.getId());

        Invitation kept = quizService.findInvitationByFromUserIdAndIsAcceptedTrue(user1.getId());

        assertEquals(i1.getId(), kept.getId());
        assertFalse(invitationRepository.findById(i2.getId()).isPresent());
    }
}
