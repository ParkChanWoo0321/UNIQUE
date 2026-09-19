package hsu.unique.game.repository;

import hsu.unique.game.entity.GameResult;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    boolean existsByEventDate(LocalDate eventDate);

    Optional<GameResult> findByEventDate(LocalDate eventDate);
}
