package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InvitationDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.QuizMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.FlashcardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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

        Invitation saved = quizService.createInvitation(dto);

        assertEquals(from, saved.getFromUser());
        assertEquals(to,   saved.getToUser());
        assertEquals(1,     saved.getDecks().size());
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
        // UPDATED: stub the new saveAndFlush calls
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
        // UPDATED: stub saveAndFlush instead of save
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
}
