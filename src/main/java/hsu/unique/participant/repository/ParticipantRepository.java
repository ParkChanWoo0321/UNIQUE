package hsu.unique.participant.repository;

import hsu.unique.participant.entity.Participant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    Optional<Participant> findByParticipantToken(String participantToken);

    boolean existsByParticipantToken(String participantToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participant p where p.participantToken = :participantToken")
    Optional<Participant> findByParticipantTokenForUpdate(
            @Param("participantToken") String participantToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Participant p where p.participationCompleted = true order by p.id")
    List<Participant> findCompletedParticipantsForUpdate();
}
