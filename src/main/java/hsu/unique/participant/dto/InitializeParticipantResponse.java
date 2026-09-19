package hsu.unique.participant.dto;

import hsu.unique.participant.entity.DailyParticipation;
import java.time.LocalDate;

public record InitializeParticipantResponse(
        LocalDate eventDate,
        boolean participationCompleted,
        int baseAttempts,
        int bonusAttempts,
        int usedAttempts,
        int remainingAttempts
) {
    public static InitializeParticipantResponse from(DailyParticipation participation) {
        return new InitializeParticipantResponse(
                participation.getEventDate(),
                participation.isParticipationCompleted(),
                participation.getBaseAttempts(),
                participation.getBonusAttempts(),
                participation.getUsedAttempts(),
                participation.getRemainingAttempts());
    }
}
