package hsu.unique.game.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateEntryRequest(
        @NotNull
        LocalDate eventDate,

        @NotNull
        @Size(min = 3, max = 3)
        List<@NotNull @Min(0) @Max(9999) Integer> numbers,

        @NotBlank
        @Pattern(regexp = "^010(?:-?\\d{4}){2}$")
        String phoneNumber,

        @NotNull
        @AssertTrue
        Boolean privacyAgreed
) {
}
