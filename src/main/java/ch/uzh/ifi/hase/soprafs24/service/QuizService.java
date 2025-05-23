// QuizService.java
package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.QuizStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.repository.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InvitationDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.QuizAnswerResponseDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.QuizUpdateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.QuizUpdateMessageDTO.PlayerProgressDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.FlashcardMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.QuizMapper;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    /* ──────────────── Dependencies ──────────────── */
    private final UserService           userService;
    private final QuizRepository        quizRepository;
    private final UserRepository        userRepository;
    private final InvitationRepository  invitationRepository;
    private final DeckRepository        deckRepository;
    private final QuizMapper            quizMapper;
    private final FlashcardMapper       flashcardMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final StatisticsService     statisticsService;
    private final ScoreRepository       scoreRepository;

    public QuizService(UserService            userService,
                       QuizRepository         quizRepository,
                       UserRepository         userRepository,
                       InvitationRepository   invitationRepository,
                       DeckRepository         deckRepository,
                       QuizMapper             quizMapper,
                       FlashcardMapper        flashcardMapper,
                       SimpMessagingTemplate  messagingTemplate,
                       StatisticsService      statisticsService,
                       ScoreRepository        scoreRepository) {
        this.userService          = userService;
        this.quizRepository       = quizRepository;
        this.userRepository       = userRepository;
        this.invitationRepository = invitationRepository;
        this.deckRepository       = deckRepository;
        this.quizMapper           = quizMapper;
        this.flashcardMapper      = flashcardMapper;
        this.messagingTemplate    = messagingTemplate;
        this.statisticsService    = statisticsService;
        this.scoreRepository      = scoreRepository;
    }

    /* ╔═════════════════ Invitation section ═══════════════╗ */

    public Invitation getInvitationById(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));
    }

    public List<Invitation> getInvitationByFromUserId(Long uid) {
        return invitationRepository.findByFromUser(userService.getUserById(uid));
    }

    public List<Invitation> getInvitationByToUserId(Long uid) {
        return invitationRepository.findByToUser(userService.getUserById(uid));
    }

    @Transactional
    public void deleteInvitationById(Long invitationId) {
        Invitation inv = getInvitationById(invitationId);
        Quiz quiz = inv.getQuiz();                 // might be null

        if (quiz != null) {
            quizRepository.save(quiz);
        }
        inv.setQuiz(null);
        invitationRepository.save(inv);            // flush FK change
        invitationRepository.delete(inv);
        invitationRepository.flush();
    }


    public void ensureInvitable(User u) {
        if (u.getStatus() == UserStatus.OFFLINE || u.getStatus() == UserStatus.PLAYING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User cannot be invited while OFFLINE or PLAYING.");
        }
    }

    public Invitation createInvitation(InvitationDTO dto) {
        User from = userService.getUserById(dto.getFromUserId());
        User to   = userService.getUserById(dto.getToUserId());
        ensureInvitable(from);
        ensureInvitable(to);

        Invitation inv = new Invitation();
        inv.setFromUser(from);
        inv.setToUser(to);
        inv.setTimeLimit(dto.getTimeLimit());
        inv.setIsAccepted(false);

        /* attach decks */
        List<Deck> decks = dto.getDeckIds().stream()
                .map(id -> deckRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Deck not found: " + id)))
                .collect(Collectors.toList());
        if (decks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one deck must be selected.");
        }
        inv.setDecks(decks);
        invitationRepository.saveAndFlush(inv);
        return inv;
    }

    /** Factory: Invitation → Quiz. */
    @Transactional
    public Quiz createQuiz(Long invitationId) {
        Invitation inv = getInvitationById(invitationId);
        Quiz quiz = quizMapper.fromInvitationToEntity(inv);   // mapper saves once

        inv.setQuiz(quiz);                // set FK
        invitationRepository.saveAndFlush(inv);   // save owning side
//        quizRepository.flush();
        quizRepository.saveAndFlush(quiz);
        return quiz;  // already managed & saved
    }

    @Transactional
    public void cancelInvitationBySender(Long invitationId) {
        Invitation inv = getInvitationById(invitationId);

        if (inv.getQuiz() != null) {
            quizRepository.delete(inv.getQuiz());
            quizRepository.flush();
        }
        User from = inv.getFromUser();
        User to = inv.getFromUser();

        invitationRepository.delete(inv);
        invitationRepository.flush();

        from.setStatus(UserStatus.ONLINE);
        to.setStatus(UserStatus.ONLINE);
        userRepository.save(from);
        userRepository.save(to);
        userRepository.flush();
    }

    @Transactional
    public void confirmedInvitation(Long invitationId) {
        Invitation inv = getInvitationById(invitationId);
        Quiz quiz      = inv.getQuiz();

        quiz.setQuizStatus(QuizStatus.IN_PROGRESS);
        quiz.setStartTime(new Date());

        inv.setIsAccepted(true);
        inv.setIsAcceptedDate(new Date());

        inv.getFromUser().setStatus(UserStatus.PLAYING);
        inv.getToUser().setStatus(UserStatus.PLAYING);

        userRepository.save(inv.getFromUser());
        userRepository.save(inv.getToUser());
        userRepository.flush();
        quizRepository.saveAndFlush(quiz);
        invitationRepository.saveAndFlush(inv);
    }

    public void rejectedInvitation(Long id) {
        Invitation inv = getInvitationById(id);
        if (inv.getQuiz() != null) {
            quizRepository.delete(inv.getQuiz());
            quizRepository.flush();
        }
        invitationRepository.delete(inv);
        invitationRepository.flush();
    }

    /** Return earliest accepted invite from sender and clean the rest. */
    public Invitation findInvitationByFromUserIdAndIsAcceptedTrue(Long fromUserId) {
        // Fetch all invitations sent by this user
        List<Invitation> invitations = getInvitationByFromUserId(fromUserId);

        // Filter to include only accepted invitations
        List<Invitation> acceptedInvitations = invitations.stream()
                .filter(Invitation::getIsAccepted)
                .sorted(Comparator.comparing(Invitation::getIsAcceptedDate)) // sort by accepted date ascending
                .collect(Collectors.toList());

        // If no accepted invitations found, return null
        if (acceptedInvitations.isEmpty()) {
            return null;
        }

        // The first one is the earliest accepted invitation
        Invitation earliestAccepted = acceptedInvitations.get(0);

        // All others are late accepted invitations – considered as rejected
        List<Invitation> lateAcceptedInvitations = acceptedInvitations.subList(1, acceptedInvitations.size());

        for (Invitation lateInvitation : lateAcceptedInvitations) {
            // Delete the corresponding quiz if it exists
            if (lateInvitation.getQuiz() != null) {
                quizRepository.delete(lateInvitation.getQuiz());
                quizRepository.flush();
            }
            User toUser = lateInvitation.getToUser();
            toUser.setStatus(UserStatus.ONLINE);
            userRepository.saveAndFlush(toUser);

            // Delete the late invitation
            invitationRepository.delete(lateInvitation);
            invitationRepository.flush();
        }
        // Return the only accepted and kept invitation
        return earliestAccepted;
    }


    /* ╚═════════════════ Invitation section ═══════════════╝ */
    /* ╔═════════════════ Quiz-runtime section ══════════════╗ */

    /** Create a quiz directly from a single deck (solo or host). */
    public Quiz startQuiz(Long deckId,
                          Integer numberOfQuestions,
                          Integer timeLimit,
                          Boolean  isMultiple) {

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        List<Flashcard> all = deck.getFlashcards();
        int n = numberOfQuestions == null || numberOfQuestions <= 0
                ? all.size() : numberOfQuestions;

        if (all.size() < n) {
            n= all.size();
            // throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            //         "Not enough flashcards in the deck for " + n + " questions");
        }

        Collections.shuffle(all);
        List<Flashcard> selected = new ArrayList<>(all.subList(0, n));

        Quiz quiz = new Quiz();
        quiz.setTimeLimit(timeLimit == null ? 0 : timeLimit);
        quiz.setStartTime(new Date());
        quiz.setIsMultiple(Boolean.TRUE.equals(isMultiple));
        quiz.setQuizStatus(quiz.getIsMultiple() ? QuizStatus.WAITING : QuizStatus.IN_PROGRESS);
        quiz.getDecks().add(deck);
        quiz.setSelectedFlashcards(selected);

        deck.setQuiz(quiz);
        return quizRepository.saveAndFlush(quiz);
    }

    /** For multi-player: flip WAITING → IN_PROGRESS when second player joins. */
    public Quiz startMultiplayerIfReady(Long quizId) {
        Quiz q = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        if (q.getIsMultiple()
                && QuizStatus.WAITING.equals(q.getQuizStatus())
                && q.getDecks().size() >= 2) {
            q.setQuizStatus(QuizStatus.IN_PROGRESS);
            q.setStartTime(new Date());
            quizRepository.saveAndFlush(q);
        }
        return q;
    }

    public Flashcard getCurrentQuestion(Long quizId, Long userId) {
        Quiz q = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        if (q.getQuizStatus() != QuizStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz not in progress.");
        }

        QuizProgressStore.ProgressState prog = QuizProgressStore.getProgress(quizId, userId);
        if (prog.isFinished()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already finished this quiz.");
        }

        List<Flashcard> cards = q.getSelectedFlashcards();
        if (cards.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No questions in this quiz.");
        }

        int idx = prog.getCurrentIndex();
        if (idx >= cards.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No more questions.");
        }
        return cards.get(idx);
    }

    //    TODO Should handle the null answer case seprately but i'll just let it be counted as an extra incorrect attempt
    @Transactional
    public QuizAnswerResponseDTO processAnswerWithFeedback(
            Long quizId, Long flashcardId, String answer, Long userId) {

        /* ───── validation ───── */
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (u.getStatus() != UserStatus.PLAYING) {
            u.setStatus(UserStatus.PLAYING);
            userRepository.saveAndFlush(u);
        }

        Quiz q = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
        if (q.getQuizStatus() != QuizStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quiz not in progress.");
        }

        QuizProgressStore.ProgressState prog = QuizProgressStore.getProgress(quizId, userId);
        if (prog.isFinished()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already finished this quiz.");
        }

        List<Flashcard> cards = q.getSelectedFlashcards();
        int idx = prog.getCurrentIndex();
        if (idx >= cards.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No more questions.");
        }

        Flashcard cur = cards.get(idx);
        if (!cur.getId().equals(flashcardId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wrong flashcard ID for current question.");
        }

        /* ───── evaluate answer (may be null) ───── */
        boolean answered = answer != null && !answer.isBlank();
        boolean correct  = false;

        if (answered) {
            prog.setTotalAttempts(prog.getTotalAttempts() + 1);
            correct = cur.getAnswer().equalsIgnoreCase(answer.trim());

            if (correct) {
                prog.setTotalCorrect(prog.getTotalCorrect() + 1);

                /* update persistent Score row */
                Score s = scoreRepository.findByQuizIdAndUserId(quizId, userId);
                if (s == null) {
                    s = new Score();
                    s.setQuiz(q);
                    s.setUser(findUser(userId));
                    s.setTotalQuestions(cards.size());
                }
                s.setCorrectQuestions(s.getCorrectQuestions() + 1);
                scoreRepository.saveAndFlush(s);
            }
        }

        /* advance pointer or finish user */
        if (answered && correct && idx + 1 < cards.size()) {
            prog.setCurrentIndex(idx + 1);
        } else if ((answered && correct && idx + 1 == cards.size())
                || (!answered && idx + 1 == cards.size())) {
            // finished either by answering last card or timing-out on it
            prog.setFinished(true);
        }

        /* per-answer statistics (only when user answered) */
        if (answered) {
            long elapsed = System.currentTimeMillis() - prog.getStartTimeMillis();
            statisticsService.recordQuizStats(findUser(userId), q,
                    prog.getTotalCorrect(), prog.getTotalAttempts(), elapsed);
        }

        /* ───── end-of-quiz detection ───── */
        boolean allFinished = checkAllFinished(q);
        boolean timeExpired = q.getTimeLimit() > 0 &&
                (System.currentTimeMillis() - q.getStartTime().getTime() >= q.getTimeLimit() * 1000L);

        if (allFinished || timeExpired) {
            endOfQuiz(q);
        }

        /* ───── broadcast & response ───── */
        broadcastProgress(quizId, cards.size(), allFinished || timeExpired);

        QuizAnswerResponseDTO dto = new QuizAnswerResponseDTO();
        dto.setWasCorrect(correct);
        dto.setFinished(prog.isFinished());
        dto.setNextQuestion(
                prog.isFinished() ? null
                        : flashcardMapper.toDTO(cards.get(prog.getCurrentIndex()))
        );
        return dto;
    }

    @Transactional
    public void cancelQuiz(Long quizId) {
        Quiz q = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        endOfQuiz(q);
    }


    /* helpers ------------------------------------------------------------ */

    /* ───── end-of-quiz detection ───── */
    private void endOfQuiz(Quiz q){
        q.setQuizStatus(QuizStatus.COMPLETED);

            q.setEndTime(new Date());

            /* record stats only for players who were still active */
            QuizProgressStore.getProgressForQuiz(q.getId()).forEach(entry -> {
                QuizProgressStore.ProgressState p = entry.getProgress();
                if (!p.isFinished()) {
                    long el = System.currentTimeMillis() - p.getStartTimeMillis();
                    statisticsService.recordQuizStats(findUser(entry.getUserId()), q,
                            p.getTotalCorrect(), p.getTotalAttempts(), el);
                    p.setFinished(true);
                }
            });

            /* reset player statuses (multi-player case) */
            if (q.getScores().size() == 2){
                List<Score> scores = q.getScores();
                User u1 = scores.get(0).getUser();
                User u2 = scores.get(1).getUser();
                u1.setStatus(UserStatus.ONLINE);
                u2.setStatus(UserStatus.ONLINE);
                userRepository.saveAll(List.of(u1, u2));
                userRepository.flush();
            }
            if (q.getScores().size() == 1){
                List<Score> scores = q.getScores();
                User u1 = scores.get(0).getUser();
                u1.setStatus(UserStatus.ONLINE);
                userRepository.saveAndFlush(u1);
            }
            // if (q.getInvitation() != null) {
            //     User u1 = q.getInvitation().getFromUser();
            //     User u2 = q.getInvitation().getToUser();
            //     u1.setStatus(UserStatus.ONLINE);
            //     u2.setStatus(UserStatus.ONLINE);
            //     userRepository.saveAll(List.of(u1, u2));
            //     userRepository.flush();
            // }
            quizRepository.saveAndFlush(q);
    }


    private boolean checkAllFinished(Quiz q) {
        List<QuizProgressStore.UserProgressEntry> ps = QuizProgressStore.getProgressForQuiz(q.getId());
        return q.getIsMultiple()
                ? ps.size() >= 2 && ps.stream().allMatch(e -> e.getProgress().isFinished())
                : !ps.isEmpty() && ps.get(0).getProgress().isFinished();
    }

    private void broadcastProgress(Long quizId, int total, boolean finished) {
        List<PlayerProgressDTO> board = QuizProgressStore.getProgressForQuiz(quizId).stream()
                .map(e -> {
                    PlayerProgressDTO d = new PlayerProgressDTO();
                    d.setUserId(e.getUserId());
                    d.setScore(e.getProgress().getTotalCorrect());
                    d.setAnsweredQuestions(e.getProgress().getCurrentIndex());
                    return d;
                }).collect(Collectors.toList());

        QuizUpdateMessageDTO msg = new QuizUpdateMessageDTO();
        msg.setQuizId(quizId);
        msg.setUpdateType(finished ? "finished" : "progress");
        msg.setTotalQuestions((long) total);
        msg.setPlayerProgress(board);

        messagingTemplate.convertAndSend("/topic/quizUpdates/" + quizId, msg);
    }

    public Quiz getQuizStatus(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    /* ╚═════════════════ Quiz-runtime section ═════════════╝ */
}
