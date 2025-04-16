package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.QuizStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Deck;
import ch.uzh.ifi.hase.soprafs24.entity.Invitation;
import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.DeckRepository;
import ch.uzh.ifi.hase.soprafs24.repository.InvitationRepository;
import ch.uzh.ifi.hase.soprafs24.repository.QuizRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InvitationDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.QuizMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


@Service
@Transactional
public class QuizService {

    private final UserRepository userRepository;

    private final UserService userService;
    // private final FirebaseService firebaseService;
    private final QuizRepository quizRepository;
    private final InvitationRepository invitationRepository;
    private final QuizMapper quizMapper;
    private final DeckRepository deckRepository;

    public QuizService(UserService userService,
                        // FirebaseService firebaseService,
                        QuizRepository quizRepository, 
                        UserRepository userRepository,
                        InvitationRepository invitationRepository,
                        QuizMapper quizMapper,
                        DeckRepository deckRepository) {
        this.userService = userService;
        // this.firebaseService = firebaseService;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.quizMapper = quizMapper;
        this.deckRepository = deckRepository;
    }


    public Quiz getQuizById(Long quizId) {
        Optional<Quiz> existingQuizOpt = quizRepository.findById(quizId);
        if (existingQuizOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found");
        }
        Quiz existingQuiz = existingQuizOpt.get();

        return existingQuiz;
    }

    public void checkUserStatusForInvitation(User user) {

        if (user.getStatus()==UserStatus.OFFLINE || user.getStatus()==UserStatus.PLAYING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User cannot be OFFLINE or PLAYING.");
        }
    }

    public Invitation getInvitationById(Long invitationId) {
        Optional<Invitation> existingInvitationOpt = invitationRepository.findById(invitationId);
        if (existingInvitationOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found");
        }
        Invitation existingInvitation = existingInvitationOpt.get();

        return existingInvitation;
    }

    public List<Invitation> getInvitationByFromUserId(Long fromUserId) {
        User fromUser = userService.getUserById(fromUserId);

        List<Invitation> invitations = invitationRepository.findByFromUser(fromUser);

        return invitations;
    }

    public List<Invitation> getInvitationByToUserId(Long toUserId) {
        User toUser = userService.getUserById(toUserId);

        List<Invitation> invitations = invitationRepository.findByToUser(toUser);

        return invitations;
    }

    public void deleteInvitationById(Long invitationId) {

        Optional<Invitation> existingInvitationOpt = invitationRepository.findById(invitationId);
        if (existingInvitationOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found");
        }

        Invitation existingInvitation = existingInvitationOpt.get();

        invitationRepository.delete(existingInvitation);
    }


    public Invitation createInvitation(InvitationDTO invitationDTO) {
        User fromUser = userService.getUserById(invitationDTO.getFromUserId());
        checkUserStatusForInvitation(fromUser);
        User toUser = userService.getUserById(invitationDTO.getToUserId());
        checkUserStatusForInvitation(toUser);

        Invitation invitation = new Invitation();
        invitation.setFromUser(fromUser);
        invitation.setToUser(toUser);
        invitation.setTimeLimit(invitationDTO.getTimeLimit());
        invitation.setIsAccepted(false);

        List<Deck> managedDecks = invitationDTO.getDeckIds().stream()
            .map(deckId -> deckRepository.findById(deckId).orElseThrow(
                () -> new RuntimeException("Deck not found: " + deckId)
            ))
            .collect(Collectors.toList());

        invitation.setDecks(new ArrayList<>(managedDecks));

        invitationRepository.save(invitation);

        return invitation;

    }

    public Quiz createQuiz(Long invitationId) {

        Invitation invitation = getInvitationById(invitationId);

        Quiz quiz = quizMapper.fromInvitationToEntity(invitation);

        invitation.setQuiz(quiz);

        invitationRepository.save(invitation);
        quizRepository.save(quiz);

        return quiz;
    }

}