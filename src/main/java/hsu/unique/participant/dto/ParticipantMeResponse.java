package hsu.unique.participant.dto;

import hsu.unique.game.entity.Submission;
import hsu.unique.participant.entity.DailyParticipation;
import java.time.LocalDate;
import java.util.List;

public record ParticipantMeResponse(
        LocalDate eventDate,
        boolean participationCompleted,
        int baseAttempts,
        int bonusAttempts,
        int totalAttempts,
        int usedAttempts,
        int remainingAttempts,
        boolean phoneRegistered,
        List<SubmittedNumberResponse> submittedNumbers
) {
    public static ParticipantMeResponse of(
            DailyParticipation participation,
            List<Submission> submissions) {
        return new ParticipantMeResponse(
                participation.getEventDate(),
                participation.isParticipationCompleted(),
                participation.getBaseAttempts(),
                participation.getBonusAttempts(),
                participation.getTotalAttempts(),
                participation.getUsedAttempts(),
                participation.getRemainingAttempts(),
                participation.hasPhoneNumber(),
                submissions.stream().map(SubmittedNumberResponse::from).toList());
    }
}
