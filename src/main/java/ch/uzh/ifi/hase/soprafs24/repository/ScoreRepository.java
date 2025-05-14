package ch.uzh.ifi.hase.soprafs24.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ch.uzh.ifi.hase.soprafs24.entity.Score;

@Repository("scoreRepository")
public interface ScoreRepository extends JpaRepository<Score,Long>{
    Optional<Score> findById(Long id);

    @Query("SELECT s FROM Score s WHERE s.quiz.id = :quizId AND s.user.id = :userId")
    Score findByQuizIdAndUserId(@Param("quizId") Long quizId, @Param("userId") Long userId);

    // Score findByQuizIdAndUserId(Long quizId, Long userId);
} 
