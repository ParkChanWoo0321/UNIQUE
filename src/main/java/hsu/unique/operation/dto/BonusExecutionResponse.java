package hsu.unique.operation.dto;

import java.time.Instant;
import java.time.LocalDate;

public record BonusExecutionResponse(
        LocalDate eventDate,
        int openedBonusStages,
        int grantedAttempts,
        int bonusAttempts,
        int targetParticipantCount,
        Instant executedAt
) {
}
