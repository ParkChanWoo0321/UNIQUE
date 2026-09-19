package hsu.unique.game.dto;

import hsu.unique.participant.dto.SubmittedNumberResponse;
import java.time.LocalDate;
import java.util.List;

public record CreateEntryResponse(
        LocalDate eventDate,
        boolean participationCompleted,
        boolean phoneRegistered,
        int usedAttempts,
        int remainingAttempts,
        List<SubmittedNumberResponse> submittedNumbers
) {
}
