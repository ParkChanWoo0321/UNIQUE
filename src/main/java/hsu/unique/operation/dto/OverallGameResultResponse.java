package hsu.unique.operation.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OverallGameResultResponse(
        List<LocalDate> eventDates,
        String winningNumber,
        boolean winnerFound,
        String winnerPhoneNumber,
        Instant calculatedAt
) {
}
