package hsu.unique.operation.dto;

import java.time.Instant;
import java.time.LocalDate;

public record FinishGameResponse(
        String status,
        LocalDate eventDate,
        String winningNumber,
        boolean winnerFound,
        String winnerPhoneNumber,
        Instant calculatedAt
) {
}
