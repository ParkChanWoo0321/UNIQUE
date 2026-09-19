package hsu.unique.game.dto;

import java.time.LocalDate;

public record OpenGameResultResponse(
        String status,
        LocalDate eventDate,
        String winningNumber,
        boolean isWinner
) {
    public OpenGameResultResponse(LocalDate eventDate, String winningNumber, boolean isWinner) {
        this("OPEN", eventDate, winningNumber, isWinner);
    }
}
