package hsu.unique.participant.repository;

import hsu.unique.participant.entity.DailyParticipation;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyParticipationRepository extends JpaRepository<DailyParticipation, Long> {

    Optional<DailyParticipation> findByParticipantIdAndEventDate(Long participantId, LocalDate eventDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from DailyParticipation d
            where d.participant.id = :participantId and d.eventDate = :eventDate
            """)
    Optional<DailyParticipation> findByParticipantIdAndEventDateForUpdate(
            @Param("participantId") Long participantId,
            @Param("eventDate") LocalDate eventDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select d from DailyParticipation d
            where d.eventDate = :eventDate
            order by d.id
            """)
    List<DailyParticipation> findAllByEventDateForUpdate(@Param("eventDate") LocalDate eventDate);

    long countByEventDateAndParticipationCompletedTrue(LocalDate eventDate);

    @Query("""
            select case when count(d) > 0 then true else false end
            from DailyParticipation d
            where d.eventDate = :eventDate
              and d.participationCompleted = true
              and d.phoneNumber = :phoneNumber
              and d.participant.id <> :participantId
            """)
    boolean existsCompletedByPhoneNumberAndEventDateExcludingParticipant(
            @Param("phoneNumber") String phoneNumber,
            @Param("eventDate") LocalDate eventDate,
            @Param("participantId") Long participantId);
}
