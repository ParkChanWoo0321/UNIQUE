package hsu.unique.game.dto;

import java.time.LocalDate;

public record NotCalculatedGameResultResponse(String status, LocalDate eventDate) {

    public NotCalculatedGameResultResponse(LocalDate eventDate) {
        this("NOT_CALCULATED", eventDate);
    }
}
