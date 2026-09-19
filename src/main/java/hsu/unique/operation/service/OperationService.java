package hsu.unique.operation.service;

import hsu.unique.common.config.EventDateResolver;
import hsu.unique.common.exception.BusinessException;
import hsu.unique.common.exception.ErrorCode;
import hsu.unique.common.util.NumberFormatter;
import hsu.unique.game.entity.GameResult;
import hsu.unique.game.entity.Submission;
import hsu.unique.game.repository.GameResultRepository;
import hsu.unique.game.repository.SubmissionRepository;
import hsu.unique.game.service.GameService;
import hsu.unique.operation.dto.BonusExecutionResponse;
import hsu.unique.operation.dto.FinishGameResponse;
import hsu.unique.operation.entity.BonusEvent;
import hsu.unique.operation.repository.BonusEventRepository;
import hsu.unique.participant.entity.DailyParticipation;
import hsu.unique.participant.repository.DailyParticipationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationService {

    private final DailyParticipationRepository dailyParticipationRepository;
    private final SubmissionRepository submissionRepository;
    private final BonusEventRepository bonusEventRepository;
    private final GameResultRepository gameResultRepository;
    private final EventDateResolver eventDateResolver;

    public OperationService(
            DailyParticipationRepository dailyParticipationRepository,
            SubmissionRepository submissionRepository,
            BonusEventRepository bonusEventRepository,
            GameResultRepository gameResultRepository,
            EventDateResolver eventDateResolver) {
        this.dailyParticipationRepository = dailyParticipationRepository;
        this.submissionRepository = submissionRepository;
        this.bonusEventRepository = bonusEventRepository;
        this.gameResultRepository = gameResultRepository;
        this.eventDateResolver = eventDateResolver;
    }

    @Transactional
    public BonusExecutionResponse executeBonus(LocalDate requestedDate) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        int openedStages = Math.toIntExact(bonusEventRepository.countByEventDate(eventDate));
        if (openedStages >= GameService.MAX_BONUS_STAGES) {
            throw new BusinessException(ErrorCode.BONUS_STAGE_LIMIT_REACHED);
        }

        int nextStage = openedStages + 1;
        List<DailyParticipation> targets = dailyParticipationRepository.findAllByEventDateForUpdate(eventDate);
        int participantCount = Math.toIntExact(
                dailyParticipationRepository.countByEventDateAndParticipationCompletedTrue(eventDate));
        Instant executedAt = Instant.now();
        BonusEvent event = BonusEvent.executed(eventDate, nextStage, participantCount, executedAt);
        try {
            bonusEventRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.BONUS_EVENT_ALREADY_EXECUTED);
        }

        targets.forEach(DailyParticipation::grantBonusAttempt);

        return new BonusExecutionResponse(
                eventDate,
                nextStage,
                1,
                nextStage,
                participantCount,
                executedAt);
    }

    @Transactional
    public FinishGameResponse finishGame(LocalDate requestedDate) {
        LocalDate eventDate = eventDateResolver.resolve(requestedDate);
        if (gameResultRepository.existsByEventDate(eventDate)) {
            throw new BusinessException(ErrorCode.GAME_RESULT_ALREADY_CALCULATED);
        }

        Integer winningNumber = submissionRepository.findSmallestUniqueNumber(eventDate).orElse(null);
        Submission winningSubmission = winningNumber == null
                ? null
                : submissionRepository.findFirstByEventDateAndNumberValue(eventDate, winningNumber).orElseThrow();
        Instant calculatedAt = Instant.now();
        GameResult result = GameResult.create(eventDate, winningNumber, winningSubmission, calculatedAt);
        try {
            gameResultRepository.saveAndFlush(result);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.GAME_RESULT_ALREADY_CALCULATED);
        }

        return new FinishGameResponse(
                "OPEN",
                eventDate,
                winningNumber == null ? null : NumberFormatter.toFourDigits(winningNumber),
                winningSubmission != null,
                winningSubmission == null
                        ? null
                        : dailyParticipationRepository
                                .findByParticipantIdAndEventDate(
                                        winningSubmission.getParticipant().getId(), eventDate)
                                .orElseThrow()
                                .getPhoneNumber(),
                calculatedAt);
    }
}
