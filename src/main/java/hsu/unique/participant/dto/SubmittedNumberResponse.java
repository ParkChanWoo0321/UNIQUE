package hsu.unique.participant.dto;

import hsu.unique.common.util.NumberFormatter;
import hsu.unique.game.entity.Submission;
import java.time.Instant;

public record SubmittedNumberResponse(String number, Instant submittedAt) {

    public static SubmittedNumberResponse from(Submission submission) {
        return new SubmittedNumberResponse(
                NumberFormatter.toFourDigits(submission.getNumberValue()),
                submission.getSubmittedAt());
    }
}
