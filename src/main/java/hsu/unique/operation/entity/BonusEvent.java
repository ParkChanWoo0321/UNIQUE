package hsu.unique.operation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "bonus_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bonus_event_date_stage",
                columnNames = {"event_date", "stage"}))
public class BonusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, updatable = false, length = 50)
    private String eventKey;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "stage")
    private Integer stage;

    @Column(name = "bonus_attempts", nullable = false)
    private int bonusAttempts;

    @Column(name = "target_participant_count", nullable = false)
    private int targetParticipantCount;

    @Column(name = "executed", nullable = false)
    private boolean executed;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BonusEvent() {
    }

    public static BonusEvent executed(
            LocalDate eventDate,
            int stage,
            int targetParticipantCount,
            Instant executedAt) {
        BonusEvent event = new BonusEvent();
        event.eventKey = "FESTIVAL_BONUS:" + eventDate + ":" + stage;
        event.eventDate = eventDate;
        event.stage = stage;
        event.bonusAttempts = 1;
        event.targetParticipantCount = targetParticipantCount;
        event.executed = true;
        event.executedAt = executedAt;
        return event;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
