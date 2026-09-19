package hsu.unique.game.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GameStatusResponse(
        LocalDate eventDate,
        List<LocalDate> eventDates,
        String phase,
        long participantCount,
        int openedBonusStages,
        int maxBonusStages,
        int baseAttempts,
        int totalAttempts,
        LocalTime opensAt,
        LocalTime closesAt,
        LocalTime resultAt
) {
}
