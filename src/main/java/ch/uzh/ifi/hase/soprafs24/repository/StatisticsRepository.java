package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository("statisticsRepository")
public interface StatisticsRepository extends JpaRepository<Statistics, Long> {
    // Additional query methods (if needed) can be defined here.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Statistics s "
            + "where s.quiz.id = :quizId and s.user.id = :userId")
    Optional<Statistics> lockByQuizIdAndUserId(
        @Param("quizId") Long quizId,
        @Param("userId") Long userId);
    List<Statistics> findByQuiz_Id(Long quizId);
    List<Statistics> findByUser_Id(Long userId);

}
