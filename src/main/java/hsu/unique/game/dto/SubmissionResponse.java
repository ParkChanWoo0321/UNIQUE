package hsu.unique.game.dto;

import hsu.unique.common.util.NumberFormatter;
import hsu.unique.game.entity.Submission;
import hsu.unique.participant.entity.DailyParticipation;
import java.time.Instant;
import java.time.LocalDate;

public record SubmissionResponse(
        Long submissionId,
        LocalDate eventDate,
        String number,
        int usedAttempts,
        int remainingAttempts,
        Instant submittedAt
) {
    public static SubmissionResponse of(Submission submission, DailyParticipation participation) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getEventDate(),
                NumberFormatter.toFourDigits(submission.getNumberValue()),
                participation.getUsedAttempts(),
                participation.getRemainingAttempts(),
                submission.getSubmittedAt());
    }
}
