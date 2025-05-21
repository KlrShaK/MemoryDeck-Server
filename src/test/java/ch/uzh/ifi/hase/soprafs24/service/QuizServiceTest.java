package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.QuizStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Flashcard;
import ch.uzh.ifi.hase.soprafs24.entity.Invitation;
import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.InvitationRepository;
import ch.uzh.ifi.hase.soprafs24.repository.QuizRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InvitationDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.QuizMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuizServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private QuizRepository quizRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private QuizMapper quizMapper;

    @InjectMocks private QuizService quizService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getInvitationByFromUserId_returnsInvitations() {
        User fromUser = new User();
        fromUser.setId(1L);
        List<Invitation> invitations = List.of(new Invitation());

        when(userService.getUserById(1L)).thenReturn(fromUser);
        when(invitationRepository.findByFromUser(fromUser)).thenReturn(invitations);

        List<Invitation> result = quizService.getInvitationByFromUserId(1L);

        assertEquals(1, result.size());
        verify(invitationRepository).findByFromUser(fromUser);
    }

    @Test
    void getInvitationByToUserId_returnsInvitations() {
        User toUser = new User();
        toUser.setId(2L);
        List<Invitation> invitations = List.of(new Invitation());

        when(userService.getUserById(2L)).thenReturn(toUser);
        when(invitationRepository.findByToUser(toUser)).thenReturn(invitations);

        List<Invitation> result = quizService.getInvitationByToUserId(2L);

        assertEquals(1, result.size());
        verify(invitationRepository).findByToUser(toUser);
    }

    @Test
    void createInvitation_success() {
        InvitationDTO dto = new InvitationDTO();
        dto.setFromUserId(1L);
        dto.setToUserId(2L);
        dto.setTimeLimit(2);
        dto.setDeckIds(List.of(100L));

        User from = new User(); from.setStatus(UserStatus.ONLINE);
        User to   = new User(); to.setStatus(UserStatus.ONLINE);
        Deck deck = new Deck();

        when(userService.getUserById(1L)).thenReturn(from);
        when(userService.getUserById(2L)).thenReturn(to);
        when(deckRepository.findById(100L)).thenReturn(Optional.of(deck));
        when(invitationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Invitation saved = quizService.createInvitation(dto);

        assertEquals(from, saved.getFromUser());
        assertEquals(to,   saved.getToUser());
        assertEquals(1,    saved.getDecks().size());
        assertFalse(saved.getIsAccepted());
        verify(invitationRepository).saveAndFlush(any());
    }

    @Test
    void confirmedInvitation_success() {
        User sender   = new User(); sender.setStatus(UserStatus.ONLINE);
        User receiver = new User(); receiver.setStatus(UserStatus.ONLINE);
        Quiz quiz     = new Quiz();
        Invitation inv = new Invitation();
        inv.setFromUser(sender);
        inv.setToUser(receiver);
        inv.setQuiz(quiz);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(quizRepository.saveAndFlush(quiz)).thenReturn(quiz);
        when(invitationRepository.saveAndFlush(inv)).thenReturn(inv);

        quizService.confirmedInvitation(1L);

        assertEquals(UserStatus.PLAYING, sender.getStatus());
        assertEquals(UserStatus.PLAYING, receiver.getStatus());
        assertTrue(inv.getIsAccepted());
        assertNotNull(inv.getIsAcceptedDate());

        verify(userRepository).save(sender);
        verify(userRepository).save(receiver);
        verify(userRepository).flush();
        verify(quizRepository).saveAndFlush(quiz);
        verify(invitationRepository).saveAndFlush(inv);
    }

    @Test
    void rejectedInvitation_deletesEntities() {
        Quiz quiz = new Quiz();
        Invitation inv = new Invitation();
        inv.setQuiz(quiz);

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        quizService.rejectedInvitation(1L);

        verify(quizRepository).delete(quiz);
        verify(quizRepository).flush();
        verify(invitationRepository).delete(inv);
        verify(invitationRepository).flush();
    }

    @Test
    void findInvitationByFromUserIdAndIsAcceptedTrue_returnsEarliestAcceptedAndDeletesOthers() {
        User fromUser = new User(); fromUser.setId(1L);
        User to1 = new User(), to2 = new User();

        Invitation old = new Invitation();
        old.setIsAccepted(true);
        old.setIsAcceptedDate(new Date(1000));
        old.setToUser(to1);

        Invitation late = new Invitation();
        late.setIsAccepted(true);
        late.setIsAcceptedDate(new Date(2000));
        late.setToUser(to2);
        Quiz   q2 = new Quiz();
        late.setQuiz(q2);

        when(userService.getUserById(1L)).thenReturn(fromUser);
        when(invitationRepository.findByFromUser(fromUser)).thenReturn(List.of(old, late));
        doNothing().when(quizRepository).delete(any());

        Invitation result = quizService.findInvitationByFromUserIdAndIsAcceptedTrue(1L);

        assertEquals(old, result);
        assertEquals(UserStatus.ONLINE, to2.getStatus());

        verify(quizRepository).delete(q2);
        verify(quizRepository).flush();
        verify(userRepository).saveAndFlush(to2);
        verify(invitationRepository).delete(late);
        verify(invitationRepository).flush();
    }

    @Test
    void deleteInvitationById_deletesSuccessfully() {
        Invitation inv = new Invitation();
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        quizService.deleteInvitationById(1L);

        verify(invitationRepository).save(inv);
        verify(invitationRepository).delete(inv);
        verify(invitationRepository).flush();
    }

    @Test
    void checkUserStatusForInvitation_throwsOnOffline() {
        User user = new User();
        user.setStatus(UserStatus.OFFLINE);
        assertThrows(ResponseStatusException.class,
                () -> quizService.ensureInvitable(user));
    }

    @Test
    void checkUserStatusForInvitation_throwsOnPlaying() {
        User user = new User();
        user.setStatus(UserStatus.PLAYING);
        assertThrows(ResponseStatusException.class,
                () -> quizService.ensureInvitable(user));
    }

    @Test
    void getInvitationById_validId_returnsInvitation() {
        Invitation inv = new Invitation();
        inv.setId(1L);
        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));

        Invitation result = quizService.getInvitationById(1L);

        assertEquals(inv, result);
        verify(invitationRepository).findById(1L);
    }

    @Test
    void getInvitationById_invalidId_throwsException() {
        when(invitationRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> quizService.getInvitationById(999L));

        assertEquals("404 NOT_FOUND \"Invitation not found\"", ex.getMessage());
    }

    @Test
    void createQuiz_validInvitationId_createsQuiz() {
        Invitation inv = new Invitation();
        inv.setId(1L);
        inv.setDecks(new ArrayList<>());

        Quiz quiz = new Quiz();
        quiz.setId(10L);
        quiz.setDecks(new ArrayList<>());

        when(invitationRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(quizMapper.fromInvitationToEntity(inv)).thenReturn(quiz);
        when(invitationRepository.saveAndFlush(inv)).thenReturn(inv);
        when(quizRepository.saveAndFlush(quiz)).thenReturn(quiz);

        Quiz result = quizService.createQuiz(1L);

        assertEquals(quiz, result);
        assertEquals(quiz, inv.getQuiz());

        verify(invitationRepository).findById(1L);
        verify(quizMapper).fromInvitationToEntity(inv);
        verify(invitationRepository).saveAndFlush(inv);
        verify(quizRepository).saveAndFlush(quiz);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // startMultiplayerIfReady(...)
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    class StartMultiplayerIfReadyTests {

        @Test
        void quizNotFound_throwsNotFound() {
            when(quizRepository.findById(42L)).thenReturn(Optional.empty());
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> quizService.startMultiplayerIfReady(42L)
            );
            assertEquals("404 NOT_FOUND \"Quiz not found\"", ex.getMessage());
        }

        @Test
        void notMultipleOrWrongStatus_orTooFewDecks_returnsUnmodifiedQuiz() {
            Quiz q = new Quiz();
            q.setId(1L);
            q.setIsMultiple(false);
            q.setQuizStatus(QuizStatus.WAITING);
            q.setDecks(Collections.emptyList());

            when(quizRepository.findById(1L)).thenReturn(Optional.of(q));

            Quiz result = quizService.startMultiplayerIfReady(1L);
            verify(quizRepository, never()).saveAndFlush(any());
            assertSame(q, result);
            assertEquals(QuizStatus.WAITING, result.getQuizStatus());
        }

        @Test
        void ready_multiplayer_waiting_andAtLeastTwoDecks_updatesStatusAndSaves() {
            Quiz q = new Quiz();
            q.setId(2L);
            q.setIsMultiple(true);
            q.setQuizStatus(QuizStatus.WAITING);
            // CORRECTED: use Deck instances instead of Object
            q.setDecks(new ArrayList<>(List.of(new Deck(), new Deck())));

            when(quizRepository.findById(2L)).thenReturn(Optional.of(q));
            when(quizRepository.saveAndFlush(q)).thenReturn(q);

            Quiz result = quizService.startMultiplayerIfReady(2L);

            verify(quizRepository).saveAndFlush(q);
            assertEquals(QuizStatus.IN_PROGRESS, result.getQuizStatus());
            assertNotNull(result.getStartTime());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // getCurrentQuestion(...)
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    class GetCurrentQuestionTests {

        private Quiz q;
        private Flashcard card1, card2;

        @BeforeEach
        void initQuiz() {
            card1 = new Flashcard(); card1.setId(10L);
            card2 = new Flashcard(); card2.setId(20L);
            q = new Quiz();
            q.setId(5L);
            q.setQuizStatus(QuizStatus.IN_PROGRESS);
            q.setSelectedFlashcards(new ArrayList<>(List.of(card1, card2)));
            when(quizRepository.findById(5L)).thenReturn(Optional.of(q));
        }

        @Test
        void quizNotFound_throwsNotFound() {
            when(quizRepository.findById(99L)).thenReturn(Optional.empty());
            assertThrows(ResponseStatusException.class,
                    () -> quizService.getCurrentQuestion(99L, 1L));
        }

        @Test
        void notInProgress_throwsBadRequest() {
            q.setQuizStatus(QuizStatus.WAITING);
            assertThrows(ResponseStatusException.class,
                    () -> quizService.getCurrentQuestion(5L, 1L),
                    "Quiz not in progress.");
        }

        @Test
        void alreadyFinished_throwsBadRequest() {
            QuizProgressStore.getProgress(5L, 100L).setFinished(true);
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> quizService.getCurrentQuestion(5L, 100L)
            );
            assertEquals("400 BAD_REQUEST \"You already finished this quiz.\"", ex.getMessage());
        }

        @Test
        void noQuestions_throwsInternalServerError() {
            q.setSelectedFlashcards(Collections.emptyList());
            assertThrows(ResponseStatusException.class,
                    () -> quizService.getCurrentQuestion(5L, 200L),
                    "No questions in this quiz.");
        }

        @Test
        void indexOutOfBounds_throwsBadRequest() {
            QuizProgressStore.getProgress(5L, 300L).setCurrentIndex(2);
            assertThrows(ResponseStatusException.class,
                    () -> quizService.getCurrentQuestion(5L, 300L),
                    "No more questions.");
        }

        @Test
        void validRequest_returnsCurrentFlashcard() {
            Flashcard result = quizService.getCurrentQuestion(5L, 400L);
            assertSame(card1, result);
            var prog = QuizProgressStore.getProgress(5L, 400L);
            prog.setCurrentIndex(1);
            assertSame(card2, quizService.getCurrentQuestion(5L, 400L));
        }
    }
}
