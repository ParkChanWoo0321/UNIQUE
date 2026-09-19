package hsu.unique.game.repository;

import hsu.unique.game.entity.Submission;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findAllByParticipantIdAndEventDateOrderBySubmittedAtAsc(
            Long participantId,
            LocalDate eventDate);

    Optional<Submission> findFirstByEventDateAndNumberValue(LocalDate eventDate, Integer numberValue);

    long countByEventDateAndNumberValue(LocalDate eventDate, Integer numberValue);

    boolean existsByParticipantIdAndEventDateAndNumberValue(
            Long participantId,
            LocalDate eventDate,
            Integer numberValue);

    @Query(value = """
            SELECT number_value
            FROM submissions
            WHERE event_date = :eventDate
            GROUP BY number_value
            HAVING COUNT(*) = 1
            ORDER BY number_value ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Integer> findSmallestUniqueNumber(@Param("eventDate") LocalDate eventDate);
}
