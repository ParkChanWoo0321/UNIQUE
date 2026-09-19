package hsu.unique.operation.repository;

import hsu.unique.operation.entity.BonusEvent;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusEventRepository extends JpaRepository<BonusEvent, Long> {

    long countByEventDate(LocalDate eventDate);

    boolean existsByEventDateAndStage(LocalDate eventDate, Integer stage);

    Optional<BonusEvent> findFirstByEventDateOrderByStageDesc(LocalDate eventDate);
}
