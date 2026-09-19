package hsu.unique.participant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "daily_participations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_participation_participant_date",
                        columnNames = {"participant_id", "event_date"}),
                @UniqueConstraint(
                        name = "uk_daily_participation_date_phone",
                        columnNames = {"event_date", "phone_number"})
        },
        indexes = {
                @Index(name = "idx_daily_participation_date", columnList = "event_date"),
                @Index(name = "idx_daily_participation_completed", columnList = "event_date, participation_completed")
        })
public class DailyParticipation {

    public static final int DEFAULT_BASE_ATTEMPTS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "base_attempts", nullable = false)
    private int baseAttempts;

    @Column(name = "bonus_attempts", nullable = false)
    private int bonusAttempts;

    @Column(name = "used_attempts", nullable = false)
    private int usedAttempts;

    @Column(name = "participation_completed", nullable = false)
    private boolean participationCompleted;

    @Column(name = "phone_number", length = 11)
    private String phoneNumber;

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

    protected DailyParticipation() {
    }

    public static DailyParticipation create(
            Participant participant,
            LocalDate eventDate,
            int openedBonusAttempts) {
        DailyParticipation participation = new DailyParticipation();
        participation.participant = participant;
        participation.eventDate = eventDate;
        participation.baseAttempts = DEFAULT_BASE_ATTEMPTS;
        participation.bonusAttempts = openedBonusAttempts;
        participation.usedAttempts = 0;
        participation.participationCompleted = false;
        participation.privacyAgreed = false;
        return participation;
    }

    public int getTotalAttempts() {
        return baseAttempts + bonusAttempts;
    }

    public int getRemainingAttempts() {
        return getTotalAttempts() - usedAttempts;
    }

    public void useAttempts(int attempts) {
        this.usedAttempts += attempts;
    }

    public void completeInitialEntry() {
        this.participationCompleted = true;
    }

    public boolean hasPhoneNumber() {
        return phoneNumber != null && !phoneNumber.isBlank();
    }

    public void registerPhone(String normalizedPhoneNumber, Instant agreedAt) {
        this.phoneNumber = normalizedPhoneNumber;
        this.privacyAgreed = true;
        this.privacyAgreedAt = agreedAt;
    }

    public void grantBonusAttempt() {
        this.bonusAttempts += 1;
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
