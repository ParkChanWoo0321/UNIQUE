package hsu.unique.participant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;

@Getter
@Entity
@Table(name = "participants", indexes = {
        @Index(name = "idx_participant_completed", columnList = "participation_completed")
})
public class Participant {

    public static final int DEFAULT_BASE_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant_token", nullable = false, unique = true, length = 64)
    private String participantToken;

    @Column(name = "phone_number", length = 11)
    private String phoneNumber;

    // These fields are retained only so the application remains compatible with
    // the schema created by the previous version. DailyParticipation is now the
    // authoritative source for per-day attempts, completion and contact data.
    @Column(name = "base_attempts", nullable = false)
    private int baseAttempts;

    @Column(name = "bonus_attempts", nullable = false)
    private int bonusAttempts;

    @Column(name = "used_attempts", nullable = false)
    private int usedAttempts;

    @Column(name = "participation_completed", nullable = false)
    private boolean participationCompleted;

    @Column(name = "privacy_agreed", nullable = false)
    private boolean privacyAgreed;

    @Column(name = "privacy_agreed_at")
    private Instant privacyAgreedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Participant() {
    }

    public static Participant create(String participantToken) {
        Participant participant = new Participant();
        participant.participantToken = participantToken;
        participant.baseAttempts = DEFAULT_BASE_ATTEMPTS;
        participant.bonusAttempts = 0;
        participant.usedAttempts = 0;
        participant.participationCompleted = false;
        participant.privacyAgreed = false;
        return participant;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
