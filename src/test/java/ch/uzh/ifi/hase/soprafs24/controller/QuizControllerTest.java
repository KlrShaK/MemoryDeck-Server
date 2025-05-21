package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.*;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.*;
import ch.uzh.ifi.hase.soprafs24.service.QuizService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuizController.class)
class QuizControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;

    @MockBean private QuizService       quizService;
    @MockBean private QuizMapper        quizMapper;
    @MockBean private InvitationMapper  invitationMapper;
    @MockBean private FlashcardMapper   flashcardMapper;

    // ───────────── Invitation endpoints ─────────────
    @Nested
    class InvitationEndpoints {

        @Test
        void sendQuizInvitation_returnsQuizDTO() throws Exception {
            InvitationDTO dto = new InvitationDTO();
            dto.setFromUserId(1L);
            dto.setToUserId(2L);
            dto.setTimeLimit(5);

            Invitation inv = new Invitation();
            inv.setId(11L);
            Quiz quiz = new Quiz();
            quiz.setId(22L);
            QuizDTO out = new QuizDTO();
            out.setId(22L);

            when(quizService.createInvitation(any())).thenReturn(inv);
            when(quizService.createQuiz(11L)).thenReturn(quiz);
            when(quizMapper.convertEntityToDTO(quiz)).thenReturn(out);

            mockMvc.perform(post("/quiz/invitation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(22));

            verify(quizService).createInvitation(any());
            verify(quizService).createQuiz(11L);
        }

        @Test
        void getQuizInvitation_returnsDTO() throws Exception {
            Invitation inv = new Invitation();
            inv.setId(33L);
            InvitationDTO out = new InvitationDTO();
            out.setId(33L);

            when(quizService.getInvitationById(33L)).thenReturn(inv);
            when(invitationMapper.toDTO(inv)).thenReturn(out);

            mockMvc.perform(get("/quiz/invitation/{id}", 33L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(33));
        }

        @Test
        void getInvitesBySender_returnsList() throws Exception {
            Invitation inv = new Invitation();
            inv.setId(44L);
            InvitationDTO dto = new InvitationDTO();
            dto.setId(44L);

            when(quizService.getInvitationByFromUserId(5L))
                    .thenReturn(List.of(inv));
            when(invitationMapper.toDTOList(anyList()))
                    .thenReturn(List.of(dto));

            mockMvc.perform(get("/quiz/invitation/senders")
                            .param("fromUserId","5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(44));
        }

        @Test
        void senderCancelled_callsService() throws Exception {
            mockMvc.perform(delete("/quiz/invitation/senders/cancel")
                            .param("invitationId","66"))
                    .andExpect(status().isOk());
            verify(quizService).cancelInvitationBySender(66L);
        }

        @Test
        void getInvitesByReceiver_returnsList() throws Exception {
            Invitation inv = new Invitation(); inv.setId(77L);
            InvitationDTO dto = new InvitationDTO(); dto.setId(77L);

            when(quizService.getInvitationByToUserId(6L))
                    .thenReturn(List.of(inv));
            when(invitationMapper.toDTOList(anyList()))
                    .thenReturn(List.of(dto));

            mockMvc.perform(get("/quiz/invitation/receivers")
                            .param("toUserId","6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(77));
        }

        @Test
        void confirmInvite_callsService() throws Exception {
            mockMvc.perform(get("/quiz/response/confirmation")
                            .param("invitationId","88"))
                    .andExpect(status().isOk());
            verify(quizService).confirmedInvitation(88L);
        }

        @Test
        void rejectInvite_callsService() throws Exception {
            mockMvc.perform(delete("/quiz/response/rejection")
                            .param("invitationId","99"))
                    .andExpect(status().isOk());
            verify(quizService).rejectedInvitation(99L);
        }

        @Test
        void acceptedInvite_whenPresent_returnsDTO() throws Exception {
            Invitation inv = new Invitation(); inv.setId(123L);
            InvitationDTO dto = new InvitationDTO(); dto.setId(123L);

            when(quizService.findInvitationByFromUserIdAndIsAcceptedTrue(7L))
                    .thenReturn(inv);
            when(invitationMapper.toDTO(inv)).thenReturn(dto);

            mockMvc.perform(get("/quiz/invitation/accepted")
                            .param("fromUserId","7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(123));
        }

        @Test
        void acceptedInvite_whenNull_returnsEmptyBody() throws Exception {
            when(quizService.findInvitationByFromUserIdAndIsAcceptedTrue(8L))
                    .thenReturn(null);

            mockMvc.perform(get("/quiz/invitation/accepted")
                            .param("fromUserId","8"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        @Test
        void deleteInvitation_callsService() throws Exception {
            mockMvc.perform(delete("/quiz/invitation/delete/{id}", 55L))
                    .andExpect(status().isOk());
            verify(quizService).deleteInvitationById(55L);
        }
    }

    // ───────────── Quiz endpoints ─────────────
    @Nested
    class QuizFlowEndpoints {

        @Test
        void startQuiz_requiresDeckId() throws Exception {
            QuizStartRequestDTO req = new QuizStartRequestDTO();
            // leave deckId null
            mockMvc.perform(post("/quiz/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Throwable ex = result.getResolvedException();
                        assertThat(ex).isInstanceOf(ResponseStatusException.class);
                        assertThat(((ResponseStatusException)ex).getReason())
                                .contains("Deck ID is required");
                    });
        }

        @Test
        void startQuiz_success_returnsQuizDTO() throws Exception {
            QuizStartRequestDTO req = new QuizStartRequestDTO();
            req.setDeckId(5L);
            req.setNumberOfQuestions(2);
            req.setTimeLimit(60);
            req.setIsMultiple(true);

            Quiz quiz = new Quiz(); quiz.setId(9L);
            QuizDTO dto = new QuizDTO(); dto.setId(9L);

            when(quizService.startQuiz(5L,2,60,true)).thenReturn(quiz);
            when(quizMapper.convertEntityToDTO(quiz)).thenReturn(dto);

            mockMvc.perform(post("/quiz/start")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(9));
        }

        @Test
        void answer_missingFields_throwsBadRequest() throws Exception {
            QuizAnswerRequestDTO req = new QuizAnswerRequestDTO();
            req.setQuizId(1L);
            // missing flashcardId and userId
            mockMvc.perform(post("/quiz/answer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Throwable ex = result.getResolvedException();
                        assertThat(ex).isInstanceOf(ResponseStatusException.class);
                        assertThat(((ResponseStatusException)ex).getReason())
                                .contains("Missing required fields");
                    });
        }

        @Test
        void answer_success_returnsResponseDTO() throws Exception {
            QuizAnswerRequestDTO req = new QuizAnswerRequestDTO();
            req.setQuizId(1L);
            req.setFlashcardId(2L);
            req.setUserId(3L);
            req.setSelectedAnswer("X");

            QuizAnswerResponseDTO out = new QuizAnswerResponseDTO();
            out.setWasCorrect(true);
            out.setFinished(false);

            when(quizService.processAnswerWithFeedback(1L,2L,"X",3L))
                    .thenReturn(out);

            mockMvc.perform(post("/quiz/answer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.wasCorrect").value(true))
                    .andExpect(jsonPath("$.finished").value(false));
        }

        @Test
        void currentQuestion_returnsFlashcardDTO() throws Exception {
            Flashcard fc = new Flashcard(); fc.setId(15L);
            FlashcardDTO dto = new FlashcardDTO(); dto.setId(15L);

            when(quizService.getCurrentQuestion(7L,8L)).thenReturn(fc);
            when(flashcardMapper.toDTO(fc)).thenReturn(dto);

            mockMvc.perform(get("/quiz/{quizId}/currentQuestion",7L)
                            .param("userId","8"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(15));
        }

        @Test
        void status_returnsQuizDTO() throws Exception {
            Quiz quiz = new Quiz(); quiz.setId(20L);
            QuizDTO dto = new QuizDTO(); dto.setId(20L);

            when(quizService.getQuizStatus(20L)).thenReturn(quiz);
            when(quizMapper.convertEntityToDTO(quiz)).thenReturn(dto);

            mockMvc.perform(get("/quiz/status/{id}",20L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(20));
        }

        @Test
        void quitGame_callsService() throws Exception {
            mockMvc.perform(delete("/quiz/quit/{quizId}",77L))
                    .andExpect(status().isOk());
            verify(quizService).cancelQuiz(77L);
        }
    }
}
