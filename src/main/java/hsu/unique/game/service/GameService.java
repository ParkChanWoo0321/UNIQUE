package hsu.unique.game.service;

import hsu.unique.common.config.EventDateResolver;
import hsu.unique.common.exception.BusinessException;
import hsu.unique.common.exception.ErrorCode;
import hsu.unique.common.util.NumberFormatter;
import hsu.unique.common.util.PhoneNumberNormalizer;
import hsu.unique.game.dto.CreateEntryRequest;
import hsu.unique.game.dto.CreateEntryResponse;
import hsu.unique.game.dto.GameStatusResponse;
import hsu.unique.game.dto.NotCalculatedGameResultResponse;
import hsu.unique.game.dto.OpenGameResultResponse;
import hsu.unique.game.dto.SubmissionResponse;
import hsu.unique.game.entity.GameResult;
import hsu.unique.game.entity.Submission;
import hsu.unique.game.repository.GameResultRepository;
import hsu.unique.game.repository.SubmissionRepository;
import hsu.unique.operation.repository.BonusEventRepository;
import hsu.unique.participant.dto.SubmittedNumberResponse;
import hsu.unique.participant.entity.DailyParticipation;
import hsu.unique.participant.entity.Participant;
import hsu.unique.participant.repository.DailyParticipationRepository;
import hsu.unique.participant.repository.ParticipantRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    public static final int MAX_BONUS_STAGES = 4;

    private final ParticipantRepository participantRepository;
    private final DailyParticipationRepository dailyParticipationRepository;
    private final SubmissionRepository submissionRepository;
    private final GameResultRepository gameResultRepository;
    private final BonusEventRepository bonusEventRepository;
    private final EventDateResolver eventDateResolver;
    private final ZoneId eventZoneId;
    private final LocalTime opensAt;
    private final LocalTime closesAt;
    private final LocalTime resultAt;
    private final boolean enforceSchedule;

    public GameService(
            ParticipantRepository participantRepository,
            DailyParticipationRepository dailyParticipationRepository,
            SubmissionRepository submissionRepository,
            GameResultRepository gameResultRepository,
            BonusEventRepository bonusEventRepository,
            EventDateResolver eventDateResolver,
            @Value("${app.event.zone-id:Asia/Seoul}") String eventZoneId,
            @Value("${app.event.open-at:09:00}") LocalTime opensAt,
            @Value("${app.event.close-at:16:00}") LocalTime closesAt,
            @Value("${app.event.result-at:17:00}") LocalTime resultAt,
            @Value("${app.event.enforce-schedule:false}") boolean enforceSchedule) {
        this.participantRepository = participantRepository;
        this.dailyParticipationRepository = dailyParticipationRepository;
        this.submissionRepository = submissionRepository;
        this.gameResultRepository = gameResultRepository;
        this.bonusEventRepository = bonusEventRepository;
        this.eventDateResolver = eventDateResolver;
        this.eventZoneId = ZoneId.of(eventZoneId);
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.resultAt = resultAt;
        this.enforceSchedule = enforceSchedule;
    }

    @Transactional
    public CreateEntryResponse createEntry(String cookieToken, CreateEntryRequest request) {
        LocalDate eventDate = eventDateResolver.resolve(request.eventDate());
        ensureSubmissionOpen(eventDate);
        validateInitialEntry(request);

        Participant participant = getParticipantForUpdate(cookieToken);
        DailyParticipation participation = getParticipationForUpdate(participant.getId(), eventDate);
        if (participation.isParticipationCompleted() || participation.getUsedAttempts() > 0) {
            throw new BusinessException(ErrorCode.ENTRY_ALREADY_SUBMITTED);
        }

        String phoneNumber = PhoneNumberNormalizer.normalize(request.phoneNumber());
        if (dailyParticipationRepository.existsCompletedByPhoneNumberAndEventDateExcludingParticipant(
                phoneNumber, eventDate, participant.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        participation.registerPhone(phoneNumber, Instant.now());
        participation.useAttempts(DailyParticipation.DEFAULT_BASE_ATTEMPTS);
        participation.completeInitialEntry();
        try {
            dailyParticipationRepository.saveAndFlush(participation);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        List<Submission> submissions = request.numbers().stream()
                .map(number -> Submission.create(participant, eventDate, number))
                .toList();
        submissionRepository.saveAllAndFlush(submissions);

        return new CreateEntryResponse(
                eventDate,
                true,
                true,
                participation.getUsedAttempts(),
                participation.getRemainingAttempts(),
                submissions.stream().map(SubmittedNumberResponse::from).toList());
    }

    @Transactional
    public SubmissionResponse submitBonusNumber(
            String cookieToken,
            LocalDate requestedDate,
            Integer number) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        ensureSubmissionOpen(eventDate);
        validateNumber(number);

        Participant participant = getParticipantForUpdate(cookieToken);
        DailyParticipation participation = getParticipationForUpdate(participant.getId(), eventDate);
        if (!participation.isParticipationCompleted()) {
            throw new BusinessException(ErrorCode.INITIAL_ENTRY_REQUIRED);
        }
        if (participation.getRemainingAttempts() <= 0) {
            throw new BusinessException(ErrorCode.NO_ATTEMPTS_LEFT);
        }
        if (submissionRepository.existsByParticipantIdAndEventDateAndNumberValue(
                participant.getId(), eventDate, number)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NUMBER_FOR_PARTICIPANT);
        }

        Submission submission = submissionRepository.saveAndFlush(
                Submission.create(participant, eventDate, number));
        participation.useAttempts(1);
        return SubmissionResponse.of(submission, participation);
    }

    @Transactional(readOnly = true)
    public Object getResult(String cookieToken, LocalDate requestedDate) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        Participant participant = getParticipant(cookieToken);
        return gameResultRepository.findByEventDate(eventDate)
                .<Object>map(result -> new OpenGameResultResponse(
                        eventDate,
                        result.getWinningNumber() == null
                                ? null
                                : NumberFormatter.toFourDigits(result.getWinningNumber()),
                        result.getWinnerParticipant() != null
                                && result.getWinnerParticipant().getId().equals(participant.getId())))
                .orElseGet(() -> new NotCalculatedGameResultResponse(eventDate));
    }

    @Transactional(readOnly = true)
    public GameStatusResponse getStatus(LocalDate requestedDate) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        int openedStages = Math.toIntExact(bonusEventRepository.countByEventDate(eventDate));
        return new GameStatusResponse(
                eventDate,
                eventDateResolver.getEventDates(),
                calculatePhase(eventDate),
                dailyParticipationRepository.countByEventDateAndParticipationCompletedTrue(eventDate),
                openedStages,
                MAX_BONUS_STAGES,
                DailyParticipation.DEFAULT_BASE_ATTEMPTS,
                DailyParticipation.DEFAULT_BASE_ATTEMPTS + openedStages,
                opensAt,
                closesAt,
                resultAt);
    }

    private void validateInitialEntry(CreateEntryRequest request) {
        if (!Boolean.TRUE.equals(request.privacyAgreed())) {
            throw new BusinessException(ErrorCode.PRIVACY_AGREEMENT_REQUIRED);
        }
        if (request.numbers() == null || request.numbers().size() != DailyParticipation.DEFAULT_BASE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.INITIAL_ENTRY_REQUIRES_THREE_NUMBERS);
        }
        request.numbers().forEach(this::validateNumber);
        if (request.numbers().stream().distinct().count() != DailyParticipation.DEFAULT_BASE_ATTEMPTS) {
            throw new BusinessException(ErrorCode.DUPLICATE_NUMBERS);
        }
    }

    private void validateNumber(Integer number) {
        if (number == null || number < 0 || number > 9999) {
            throw new BusinessException(ErrorCode.INVALID_NUMBER);
        }
    }

    private void ensureSubmissionOpen(LocalDate eventDate) {
        if (gameResultRepository.existsByEventDate(eventDate)) {
            throw new BusinessException(ErrorCode.GAME_CLOSED);
        }
        if (enforceSchedule && !"OPEN".equals(calculatePhase(eventDate))) {
            throw new BusinessException(ErrorCode.GAME_NOT_OPEN);
        }
    }

    private String calculatePhase(LocalDate eventDate) {
        if (gameResultRepository.existsByEventDate(eventDate)) {
            return "RESULT_OPEN";
        }
        ZonedDateTime now = ZonedDateTime.now(eventZoneId);
        if (eventDate.isAfter(now.toLocalDate())) {
            return "UPCOMING";
        }
        if (eventDate.isBefore(now.toLocalDate())) {
            return "COUNTING";
        }
        if (now.toLocalTime().isBefore(opensAt)) {
            return "UPCOMING";
        }
        if (now.toLocalTime().isBefore(closesAt)) {
            return "OPEN";
        }
        return "COUNTING";
    }

    private Participant getParticipant(String cookieToken) {
        if (cookieToken == null || cookieToken.isBlank()) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        return participantRepository.findByParticipantToken(cookieToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private Participant getParticipantForUpdate(String cookieToken) {
        if (cookieToken == null || cookieToken.isBlank()) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }
        return participantRepository.findByParticipantTokenForUpdate(cookieToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND));
    }

    private DailyParticipation getParticipationForUpdate(Long participantId, LocalDate eventDate) {
        return dailyParticipationRepository.findByParticipantIdAndEventDateForUpdate(participantId, eventDate)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARTICIPATION_NOT_INITIALIZED));
    }
}
