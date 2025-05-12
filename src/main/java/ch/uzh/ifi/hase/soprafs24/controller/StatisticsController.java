package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Statistics;
import ch.uzh.ifi.hase.soprafs24.repository.StatisticsRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.StatisticsDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.WinnerDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.StatisticsMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final StatisticsRepository statisticsRepo;
    private final StatisticsMapper     mapper;

    public StatisticsController(StatisticsRepository statisticsRepo,
                                StatisticsMapper mapper) {
        this.statisticsRepo = statisticsRepo;
        this.mapper         = mapper;
    }

    @GetMapping("/quiz/{quizId}")
    public List<StatisticsDTO> getByQuiz(@PathVariable Long quizId) {
        return statisticsRepo.findByQuiz_Id(quizId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }


    /** Mark the winner (or draw) of a quiz. */
    @PutMapping("/quiz/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setWinner(@PathVariable Long quizId,
                          @RequestBody WinnerDTO dto) {

        List<Statistics> stats = statisticsRepo.findByQuiz_Id(quizId);
        if (stats.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz stats not found");

        // reset old values
        stats.forEach(s -> s.setIsWinner(false));

        if (dto.getWinnerUserId() != null) {
            // mark the matching row
            stats.stream()
                    .filter(s -> s.getUser().getId().equals(dto.getWinnerUserId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "winnerUserId not part of this quiz"))
                    .setIsWinner(true);
        }
        statisticsRepo.saveAll(stats);
    }

    /* ────────────────────────────────────────────────
     *  NEW endpoint  →  GET  /statistics/{userId}
     *  returns all raw rows for that user
     * ──────────────────────────────────────────────── */
    @GetMapping("/{userId}")
    public List<StatisticsDTO> getByUser(@PathVariable Long userId) {
        return statisticsRepo.findByUser_Id(userId)
                .stream()
                .map(mapper::toDTO)         // reuse existing mapper
                .collect(Collectors.toList());
    }

}
