package hsu.unique.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SubmitNumberRequest(
        @NotNull
        LocalDate eventDate,

        @NotNull
        @Min(0)
        @Max(9999)
        Integer number
) {
}
