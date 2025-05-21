package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Statistics;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.StatisticsRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StatisticsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.WinnerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.StatisticsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
class StatisticsControllerTest {

    @Autowired private MockMvc   mockMvc;
    @Autowired private ObjectMapper mapper;

    @MockBean private StatisticsRepository statisticsRepo;
    @MockBean private StatisticsMapper     statisticsMapper;

    @Nested
    @DisplayName("GET /statistics/quiz/{quizId}")
    class GetByQuiz {

        @Test
        @DisplayName("returns list of DTOs for given quiz")
        void returnsDtoList() throws Exception {
            Long quizId = 42L;

            Statistics stat = new Statistics();
            stat.setId(7L);
            User u = new User(); u.setId(99L);
            stat.setUser(u);
            stat.setQuizDate(null);
            stat.setIsWinner(false);

            StatisticsDTO dto = new StatisticsDTO();
            dto.setUserId(99L);
            dto.setCorrectQuestions(3);
            dto.setQuizId(quizId);

            when(statisticsRepo.findByQuiz_Id(quizId)).thenReturn(List.of(stat));
            when(statisticsMapper.toDTO(stat)).thenReturn(dto);

            mockMvc.perform(get("/statistics/quiz/{quizId}", quizId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(99))
                    .andExpect(jsonPath("$[0].correctQuestions").value(3))
                    .andExpect(jsonPath("$[0].quizId").value(42));
        }
    }

    @Nested
    @DisplayName("PUT /statistics/quiz/{quizId}")
    class SetWinner {

        @Test
        @DisplayName("404 when no stats exist for quiz")
        void notFoundWhenEmpty() throws Exception {
            Long quizId = 100L;
            when(statisticsRepo.findByQuiz_Id(quizId)).thenReturn(List.of());

            WinnerDTO body = new WinnerDTO();
            mockMvc.perform(put("/statistics/quiz/{quizId}", quizId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> {
                        Throwable ex = result.getResolvedException();
                        assertThat(ex).isInstanceOf(ResponseStatusException.class);
                        assertThat(((ResponseStatusException) ex).getReason())
                                .isEqualTo("Quiz stats not found");
                    });
        }

        @Test
        @DisplayName("400 when winnerUserId not part of quiz")
        void badRequestWhenUserNotInStats() throws Exception {
            Long quizId = 101L;
            Statistics s = new Statistics();
            User u = new User(); u.setId(1L);
            s.setUser(u);
            when(statisticsRepo.findByQuiz_Id(quizId)).thenReturn(List.of(s));

            WinnerDTO body = new WinnerDTO();
            body.setWinnerUserId(2L);

            mockMvc.perform(put("/statistics/quiz/{quizId}", quizId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        Throwable ex = result.getResolvedException();
                        assertThat(ex).isInstanceOf(ResponseStatusException.class);
                        assertThat(((ResponseStatusException) ex).getReason())
                                .isEqualTo("winnerUserId not part of this quiz");
                    });
        }

        @Test
        @DisplayName("marks correct winner and saves all")
        void marksWinnerSuccessfully() throws Exception {
            Long quizId = 202L;

            Statistics s1 = new Statistics();
            User u1 = new User(); u1.setId(10L);
            s1.setUser(u1);
            s1.setIsWinner(true);

            Statistics s2 = new Statistics();
            User u2 = new User(); u2.setId(20L);
            s2.setUser(u2);

            when(statisticsRepo.findByQuiz_Id(quizId))
                    .thenReturn(List.of(s1, s2));

            WinnerDTO body = new WinnerDTO();
            body.setWinnerUserId(20L);

            mockMvc.perform(put("/statistics/quiz/{quizId}", quizId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isNoContent());

            verify(statisticsRepo).saveAll(argThat((Iterable<Statistics> iterable) -> {
                List<Statistics> list = new ArrayList<>();
                iterable.forEach(list::add);
                return list.size() == 2
                        && !list.get(0).getIsWinner()
                        &&  list.get(1).getIsWinner();
            }));
        }
    }

    @Nested
    @DisplayName("GET /statistics/{userId}")
    class GetByUser {

        @Test
        @DisplayName("returns list of DTOs for given user")
        void returnsDtoListForUser() throws Exception {
            Long userId = 55L;

            Statistics stat = new Statistics();
            stat.setId(5L);
            stat.setIsWinner(false);
            User u = new User(); u.setId(userId);
            stat.setUser(u);

            StatisticsDTO dto = new StatisticsDTO();
            dto.setUserId(userId);
            dto.setCorrectQuestions(7);
            dto.setQuizId(null);

            when(statisticsRepo.findByUser_Id(userId)).thenReturn(List.of(stat));
            when(statisticsMapper.toDTO(stat)).thenReturn(dto);

            mockMvc.perform(get("/statistics/{userId}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].userId").value(55))
                    .andExpect(jsonPath("$[0].correctQuestions").value(7));
        }
    }
}
