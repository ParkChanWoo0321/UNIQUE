package hsu.unique.game.entity;

import hsu.unique.participant.entity.Participant;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "submissions", indexes = {
        @Index(name = "idx_submission_date_number", columnList = "event_date, number_value"),
        @Index(name = "idx_submission_participant_date", columnList = "participant_id, event_date")
})
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "number_value", nullable = false)
    private int numberValue;

    // Nullable at the schema level so an existing development DB can add the
    // column without failing on old rows. Every new submission sets this value.
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    protected Submission() {
    }

    public static Submission create(Participant participant, LocalDate eventDate, int numberValue) {
        Submission submission = new Submission();
        submission.participant = participant;
        submission.eventDate = eventDate;
        submission.numberValue = numberValue;
        return submission;
    }

    @PrePersist
    void onCreate() {
        this.submittedAt = Instant.now();
    }
}
