package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Invitation;
import ch.uzh.ifi.hase.soprafs24.entity.Quiz;
import ch.uzh.ifi.hase.soprafs24.service.QuizService;
import ch.uzh.ifi.hase.soprafs24.rest.dto.InvitationDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.QuizDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.InvitationMapper;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.QuizMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import javax.validation.Valid;

/**
 * Quiz Controller
 * Handles quiz invitations between users.
 */
@RestController
@RequestMapping("/quiz")
public class QuizController {


    private final QuizMapper quizMapper;
    private final InvitationMapper invitationMapper;
    private final QuizService quizService;

    public QuizController(QuizService quizService, QuizMapper quizMapper, InvitationMapper invitationMapper) {
        this.quizService = quizService;
        this.quizMapper = quizMapper;
        this.invitationMapper = invitationMapper;
    }

    @PostMapping("/invitation")
    @ResponseStatus(HttpStatus.OK)
    public QuizDTO sendQuizInvitation(@RequestBody @Valid InvitationDTO invitationDTO) {
        // Invitation invitation = invitationMapper.toEntity(invitationDTO);
        Invitation invitation = quizService.createInvitation(invitationDTO);

        Quiz quiz = quizService.createQuiz(invitation.getId());

        return quizMapper.toDTO(quiz);
    }
    


}