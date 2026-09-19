package hsu.unique.participant.service;

import hsu.unique.participant.entity.Participant;
import hsu.unique.participant.entity.DailyParticipation;

public record ParticipantInitialization(
        Participant participant,
        DailyParticipation dailyParticipation,
        boolean cookieIssued) {
}
