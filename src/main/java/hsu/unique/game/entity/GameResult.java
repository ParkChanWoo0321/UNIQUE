package hsu.unique.game.entity;

import hsu.unique.participant.entity.Participant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "game_results")
public class GameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "result_key", nullable = false, unique = true, updatable = false, length = 20)
    private String resultKey;

    @Column(name = "event_date", unique = true)
    private LocalDate eventDate;

    @Column(name = "winning_number")
    private Integer winningNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_submission_id", unique = true)
    private Submission winningSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_participant_id")
    private Participant winnerParticipant;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GameResult() {
    }

    public static GameResult create(
            LocalDate eventDate,
            Integer winningNumber,
            Submission winningSubmission,
            Instant calculatedAt) {
        GameResult result = new GameResult();
        result.resultKey = "FINAL:" + eventDate;
        result.eventDate = eventDate;
        result.winningNumber = winningNumber;
        result.winningSubmission = winningSubmission;
        result.winnerParticipant = winningSubmission == null ? null : winningSubmission.getParticipant();
        result.calculatedAt = calculatedAt;
        return result;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
