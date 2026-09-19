package hsu.unique.common.config;

import hsu.unique.common.exception.BusinessException;
import hsu.unique.common.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventDateResolver {

    private final List<LocalDate> eventDates;

    public EventDateResolver(@Value("${app.event.dates}") List<LocalDate> eventDates) {
        if (eventDates == null || eventDates.isEmpty()) {
            throw new IllegalStateException("app.event.dates must contain at least one date");
        }
        this.eventDates = List.copyOf(eventDates);
    }

    public LocalDate resolve(LocalDate requestedDate) {
        LocalDate resolved = requestedDate == null ? eventDates.get(0) : requestedDate;
        if (!eventDates.contains(resolved)) {
            throw new BusinessException(ErrorCode.EVENT_DATE_INVALID);
        }
        return resolved;
    }

    public List<LocalDate> getEventDates() {
        return eventDates;
    }
}
