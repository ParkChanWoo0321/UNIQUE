package hsu.unique.participant.service;

import hsu.unique.common.config.EventDateResolver;
import hsu.unique.common.exception.BusinessException;
import hsu.unique.common.exception.ErrorCode;
import hsu.unique.game.entity.Submission;
import hsu.unique.game.repository.SubmissionRepository;
import hsu.unique.operation.repository.BonusEventRepository;
import hsu.unique.participant.dto.ParticipantMeResponse;
import hsu.unique.participant.entity.DailyParticipation;
import hsu.unique.participant.entity.Participant;
import hsu.unique.participant.repository.DailyParticipationRepository;
import hsu.unique.participant.repository.ParticipantRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final DailyParticipationRepository dailyParticipationRepository;
    private final SubmissionRepository submissionRepository;
    private final BonusEventRepository bonusEventRepository;
    private final ParticipantTokenGenerator tokenGenerator;
    private final EventDateResolver eventDateResolver;

    public ParticipantService(
            ParticipantRepository participantRepository,
            DailyParticipationRepository dailyParticipationRepository,
            SubmissionRepository submissionRepository,
            BonusEventRepository bonusEventRepository,
            ParticipantTokenGenerator tokenGenerator,
            EventDateResolver eventDateResolver) {
        this.participantRepository = participantRepository;
        this.dailyParticipationRepository = dailyParticipationRepository;
        this.submissionRepository = submissionRepository;
        this.bonusEventRepository = bonusEventRepository;
        this.tokenGenerator = tokenGenerator;
        this.eventDateResolver = eventDateResolver;
    }

    @Transactional
    public ParticipantInitialization initialize(String cookieToken, LocalDate requestedDate) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        ParticipantInitialization identity = initializeIdentity(cookieToken);
        DailyParticipation dailyParticipation = dailyParticipationRepository
                .findByParticipantIdAndEventDate(identity.participant().getId(), eventDate)
                .orElseGet(() -> dailyParticipationRepository.save(DailyParticipation.create(
                        identity.participant(),
                        eventDate,
                        Math.toIntExact(bonusEventRepository.countByEventDate(eventDate)))));
        return new ParticipantInitialization(
                identity.participant(),
                dailyParticipation,
                identity.cookieIssued());
    }

    @Transactional(readOnly = true)
    public ParticipantMeResponse getMe(String cookieToken, LocalDate requestedDate) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        Participant participant = getRequiredParticipant(cookieToken);
        DailyParticipation participation = dailyParticipationRepository
                .findByParticipantIdAndEventDate(participant.getId(), eventDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_INITIALIZED));
        List<Submission> submissions = submissionRepository
                .findAllByParticipantIdAndEventDateOrderBySubmittedAtAsc(participant.getId(), eventDate);
        return ParticipantMeResponse.of(participation, submissions);
    }

    @Transactional(readOnly = true)
    public Participant getRequiredParticipant(String cookieToken) {
        if (cookieToken == null || cookieToken.isBlank()) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        return participantRepository.findByParticipantToken(cookieToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private ParticipantInitialization initializeIdentity(String cookieToken) {
        if (cookieToken != null && !cookieToken.isBlank()) {
            return participantRepository.findByParticipantToken(cookieToken)
                    .map(participant -> new ParticipantInitialization(participant, null, false))
                    .orElseGet(this::createParticipant);
        }
        return createParticipant();
    }

    private ParticipantInitialization createParticipant() {
        String token;
        do {
            token = tokenGenerator.generate();
        } while (participantRepository.existsByParticipantToken(token));
        Participant participant = participantRepository.save(Participant.create(token));
        return new ParticipantInitialization(participant, null, true);
    }
}
