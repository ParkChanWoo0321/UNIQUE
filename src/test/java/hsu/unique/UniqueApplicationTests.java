package hsu.unique;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hsu.unique.game.repository.GameResultRepository;
import hsu.unique.game.repository.SubmissionRepository;
import hsu.unique.game.entity.GameResult;
import hsu.unique.game.entity.Submission;
import hsu.unique.operation.repository.BonusEventRepository;
import hsu.unique.participant.entity.DailyParticipation;
import hsu.unique.participant.entity.Participant;
import hsu.unique.participant.repository.DailyParticipationRepository;
import hsu.unique.participant.repository.ParticipantRepository;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UniqueApplicationTests {

    private static final LocalDate DAY_ONE = LocalDate.of(2026, 9, 21);
    private static final LocalDate DAY_TWO = LocalDate.of(2026, 9, 22);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ParticipantRepository participantRepository;
    @Autowired
    private DailyParticipationRepository dailyParticipationRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private BonusEventRepository bonusEventRepository;
    @Autowired
    private GameResultRepository gameResultRepository;

    @BeforeEach
    void cleanDatabase() {
        gameResultRepository.deleteAll();
        bonusEventRepository.deleteAll();
        submissionRepository.deleteAll();
        dailyParticipationRepository.deleteAll();
        participantRepository.deleteAll();
    }

    @Test
    void initializationUsesOneCookieAndCreatesIndependentDailyState() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/participants/initialize")
                        .queryParam("eventDate", DAY_ONE.toString()))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("HttpOnly"),
                                org.hamcrest.Matchers.containsString("SameSite=Lax"),
                                org.hamcrest.Matchers.containsString("Path=/"))))
                .andExpect(jsonPath("$.eventDate").value(DAY_ONE.toString()))
                .andExpect(jsonPath("$.baseAttempts").value(3))
                .andExpect(jsonPath("$.remainingAttempts").value(3))
                .andReturn();

        String token = tokenFrom(first);
        mockMvc.perform(post("/api/participants/initialize")
                        .queryParam("eventDate", DAY_TWO.toString())
                        .cookie(cookie(token)))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.eventDate").value(DAY_TWO.toString()))
                .andExpect(jsonPath("$.remainingAttempts").value(3));

        assertThat(participantRepository.count()).isEqualTo(1);
        assertThat(dailyParticipationRepository.count()).isEqualTo(2);
    }

    @Test
    void initialEntryAtomicallyStoresThreeDistinctNumbersAndPhone() throws Exception {
        String token = initialize(DAY_ONE);

        createEntry(token, DAY_ONE, "[0,37,9999]", "010-1234-5678", true, 200)
                .andExpect(jsonPath("$.participationCompleted").value(true))
                .andExpect(jsonPath("$.phoneRegistered").value(true))
                .andExpect(jsonPath("$.usedAttempts").value(3))
                .andExpect(jsonPath("$.remainingAttempts").value(0))
                .andExpect(jsonPath("$.submittedNumbers[0].number").value("0000"))
                .andExpect(jsonPath("$.submittedNumbers[1].number").value("0037"))
                .andExpect(jsonPath("$.submittedNumbers[2].number").value("9999"));

        Participant participant = participantRepository.findByParticipantToken(token).orElseThrow();
        DailyParticipation daily = dailyParticipationRepository
                .findByParticipantIdAndEventDate(participant.getId(), DAY_ONE).orElseThrow();
        assertThat(daily.getPhoneNumber()).isEqualTo("01012345678");
        assertThat(daily.isPrivacyAgreed()).isTrue();
        assertThat(daily.isParticipationCompleted()).isTrue();
        assertThat(submissionRepository
                .findAllByParticipantIdAndEventDateOrderBySubmittedAtAsc(participant.getId(), DAY_ONE))
                .hasSize(3);
    }

    @Test
    void invalidInitialEntryRollsBackAndDuplicatePhoneIsBlockedPerDay() throws Exception {
        String first = initialize(DAY_ONE);
        createEntry(first, DAY_ONE, "[10,10,20]", "01011112222", true, 400)
                .andExpect(jsonPath("$.code").value("DUPLICATE_NUMBERS"));
        assertThat(submissionRepository.count()).isZero();

        createEntry(first, DAY_ONE, "[10,20,30]", "01011112222", true, 200);

        String second = initialize(DAY_ONE);
        createEntry(second, DAY_ONE, "[40,50,60]", "010-1111-2222", true, 409)
                .andExpect(jsonPath("$.code").value("DUPLICATE_PHONE_NUMBER"));

        createEntry(second, DAY_ONE, "[40,50,60]", "01033334444", false, 400)
                .andExpect(jsonPath("$.code").value("PRIVACY_AGREEMENT_REQUIRED"));
        assertThat(submissionRepository.count()).isEqualTo(3);
    }

    @Test
    void bonusOpensOneStageAtATimeAndLateParticipantsReceiveOpenedStages() throws Exception {
        String first = initialize(DAY_ONE);
        createEntry(first, DAY_ONE, "[1,2,3]", "01011112222", true, 200);

        executeBonus(DAY_ONE, 200)
                .andExpect(jsonPath("$.openedBonusStages").value(1))
                .andExpect(jsonPath("$.grantedAttempts").value(1))
                .andExpect(jsonPath("$.bonusAttempts").value(1))
                .andExpect(jsonPath("$.targetParticipantCount").value(1));

        String late = initialize(DAY_ONE);
        mockMvc.perform(get("/api/participants/me")
                        .queryParam("eventDate", DAY_ONE.toString())
                        .cookie(cookie(late)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusAttempts").value(1))
                .andExpect(jsonPath("$.totalAttempts").value(4));

        executeBonus(DAY_ONE, 200).andExpect(jsonPath("$.openedBonusStages").value(2));
        executeBonus(DAY_ONE, 200).andExpect(jsonPath("$.openedBonusStages").value(3));
        executeBonus(DAY_ONE, 200).andExpect(jsonPath("$.openedBonusStages").value(4));
        executeBonus(DAY_ONE, 409)
                .andExpect(jsonPath("$.code").value("BONUS_STAGE_LIMIT_REACHED"));

        mockMvc.perform(get("/api/participants/me")
                        .queryParam("eventDate", DAY_ONE.toString())
                        .cookie(cookie(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusAttempts").value(4))
                .andExpect(jsonPath("$.totalAttempts").value(7))
                .andExpect(jsonPath("$.remainingAttempts").value(4));
    }

    @Test
    void bonusNumbersRequireInitialEntryAndCannotDuplicateOwnNumber() throws Exception {
        String token = initialize(DAY_ONE);

        submitNumber(token, DAY_ONE, 100, 409)
                .andExpect(jsonPath("$.code").value("INITIAL_ENTRY_REQUIRED"));
        createEntry(token, DAY_ONE, "[100,200,300]", "01012345678", true, 200);
        submitNumber(token, DAY_ONE, 400, 409)
                .andExpect(jsonPath("$.code").value("NO_ATTEMPTS_LEFT"));

        executeBonus(DAY_ONE, 200);
        submitNumber(token, DAY_ONE, 100, 409)
                .andExpect(jsonPath("$.code").value("DUPLICATE_NUMBER_FOR_PARTICIPANT"));
        submitNumber(token, DAY_ONE, 400, 200)
                .andExpect(jsonPath("$.number").value("0400"))
                .andExpect(jsonPath("$.remainingAttempts").value(0));
    }

    @Test
    void finishCalculatesEachDaySeparatelyAndReturnsWinnerPhoneOnlyToOperator() throws Exception {
        String winner = initialize(DAY_ONE);
        createEntry(winner, DAY_ONE, "[37,100,500]", "01055556666", true, 200);
        String other = initialize(DAY_ONE);
        createEntry(other, DAY_ONE, "[37,200,300]", "01077778888", true, 200);

        mockMvc.perform(get("/api/game/result")
                        .queryParam("eventDate", DAY_ONE.toString())
                        .cookie(cookie(winner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CALCULATED"));

        mockMvc.perform(post("/api/operations/finish")
                        .queryParam("eventDate", DAY_ONE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventDate").value(DAY_ONE.toString()))
                .andExpect(jsonPath("$.winningNumber").value("0100"))
                .andExpect(jsonPath("$.winnerFound").value(true))
                .andExpect(jsonPath("$.winnerPhoneNumber").value("01055556666"));

        mockMvc.perform(get("/api/game/result")
                        .queryParam("eventDate", DAY_ONE.toString())
                        .cookie(cookie(winner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isWinner").value(true))
                .andExpect(jsonPath("$.winnerPhoneNumber").doesNotExist());
        mockMvc.perform(get("/api/game/result")
                        .queryParam("eventDate", DAY_ONE.toString())
                        .cookie(cookie(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isWinner").value(false));

        mockMvc.perform(get("/api/game/result")
                        .queryParam("eventDate", DAY_TWO.toString())
                        .cookie(cookie(winner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_CALCULATED"));
    }

    @Test
    void overallRecalculationExcludesCrossDayDuplicatesAndPreservesDailyResults() throws Exception {
        String first = initialize(DAY_ONE);
        createEntry(first, DAY_ONE, "[1,2,8]", "01011112222", true, 200);
        String second = initialize(DAY_TWO);
        createEntry(second, DAY_TWO, "[1,2,3]", "01033334444", true, 200);
        mockMvc.perform(post("/api/operations/finish").queryParam("eventDate", DAY_ONE.toString()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/operations/finish").queryParam("eventDate", DAY_TWO.toString()))
                .andExpect(status().isOk());
        GameResult firstResult = gameResultRepository.findByEventDate(DAY_ONE).orElseThrow();
        GameResult secondResult = gameResultRepository.findByEventDate(DAY_TWO).orElseThrow();
        var submissionsBefore = submissionRepository.findAll();

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/operations/recalculate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventDates[0]").value(DAY_ONE.toString()))
                    .andExpect(jsonPath("$.eventDates[1]").value(DAY_TWO.toString()))
                    .andExpect(jsonPath("$.winningNumber").value("0003"))
                    .andExpect(jsonPath("$.winnerFound").value(true))
                    .andExpect(jsonPath("$.winnerPhoneNumber").value("01033334444"))
                    .andExpect(jsonPath("$.calculatedAt").isNotEmpty());
        }

        assertThat(gameResultRepository.count()).isEqualTo(2);
        assertThat(gameResultRepository.findByEventDate(DAY_ONE).orElseThrow())
                .usingRecursiveComparison().ignoringFields("winningSubmission", "winnerParticipant")
                .isEqualTo(firstResult);
        assertThat(gameResultRepository.findByEventDate(DAY_TWO).orElseThrow())
                .usingRecursiveComparison().ignoringFields("winningSubmission", "winnerParticipant")
                .isEqualTo(secondResult);
        assertThat(submissionRepository.findAll())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("participant")
                .containsExactlyInAnyOrderElementsOf(submissionsBefore);
        submitNumber(first, DAY_ONE, 10, 409)
                .andExpect(jsonPath("$.code").value("GAME_CLOSED"));
    }

    @Test
    void overallRecalculationUsesCurrentSubmissionsOnEveryCall() throws Exception {
        String first = initialize(DAY_ONE);
        createEntry(first, DAY_ONE, "[4,5,6]", "01011112222", true, 200);
        mockMvc.perform(post("/api/operations/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winningNumber").value("0004"));

        String second = initialize(DAY_TWO);
        createEntry(second, DAY_TWO, "[4,5,7]", "01033334444", true, 200);
        mockMvc.perform(post("/api/operations/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winningNumber").value("0006"))
                .andExpect(jsonPath("$.winnerPhoneNumber").value("01011112222"));
        assertThat(gameResultRepository.count()).isZero();
        assertThat(submissionRepository.count()).isEqualTo(6);
    }

    @Test
    void overallRecalculationCountsSameParticipantCrossDayDuplicatesAndUsesWinningDayPhone() throws Exception {
        String token = initialize(DAY_ONE);
        createEntry(token, DAY_ONE, "[0,1,9]", "01011112222", true, 200);
        mockMvc.perform(post("/api/participants/initialize")
                        .queryParam("eventDate", DAY_TWO.toString()).cookie(cookie(token)))
                .andExpect(status().isOk());
        createEntry(token, DAY_TWO, "[0,1,2]", "01033334444", true, 200);

        mockMvc.perform(post("/api/operations/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winningNumber").value("0002"))
                .andExpect(jsonPath("$.winnerPhoneNumber").value("01033334444"));
    }

    @Test
    void overallRecalculationReturnsNoWinnerWhenEveryNumberIsDuplicated() throws Exception {
        String first = initialize(DAY_ONE);
        createEntry(first, DAY_ONE, "[0,1,2]", "01011112222", true, 200);
        String second = initialize(DAY_TWO);
        createEntry(second, DAY_TWO, "[0,1,2]", "01033334444", true, 200);

        mockMvc.perform(post("/api/operations/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerFound").value(false))
                .andExpect(jsonPath("$.winningNumber").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.winnerPhoneNumber").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void overallRecalculationReturnsNoWinnerWhenThereAreNoSubmissions() throws Exception {
        mockMvc.perform(post("/api/operations/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerFound").value(false))
                .andExpect(jsonPath("$.winningNumber").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.winnerPhoneNumber").value(org.hamcrest.Matchers.nullValue()));
        assertThat(gameResultRepository.count()).isZero();
    }

    @Test
    void overallRecalculationIgnoresSubmissionsOutsideConfiguredEventDates() throws Exception {
        String token = initialize(DAY_ONE);
        createEntry(token, DAY_ONE, "[0,100,9999]", "01011112222", true, 200);
        Participant participant = participantRepository.findByParticipantToken(token).orElseThrow();
        submissionRepository.saveAndFlush(Submission.create(participant, DAY_TWO.plusDays(1), 0));
        submissionRepository.saveAndFlush(Submission.create(participant, null, 0));

        mockMvc.perform(post("/api/operations/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winningNumber").value("0000"))
                .andExpect(jsonPath("$.winnerPhoneNumber").value("01011112222"));
    }

    @Test
    void publicStatusMatchesFrontendAndCorsAllowsDeployedOrigin() throws Exception {
        String token = initialize(DAY_ONE);
        createEntry(token, DAY_ONE, "[11,12,13]", "01012345678", true, 200);
        executeBonus(DAY_ONE, 200);

        mockMvc.perform(get("/api/game/status")
                        .queryParam("eventDate", DAY_ONE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventDates[0]").value(DAY_ONE.toString()))
                .andExpect(jsonPath("$.eventDates[1]").value(DAY_TWO.toString()))
                .andExpect(jsonPath("$.participantCount").value(1))
                .andExpect(jsonPath("$.openedBonusStages").value(1))
                .andExpect(jsonPath("$.maxBonusStages").value(4))
                .andExpect(jsonPath("$.totalAttempts").value(4));

        mockMvc.perform(options("/api/game/status")
                        .header(HttpHeaders.ORIGIN, "https://festival-vault-design.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "https://festival-vault-design.vercel.app"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/game/entries']").exists())
                .andExpect(jsonPath("$.paths['/api/game/status']").exists())
                .andExpect(jsonPath("$.paths['/api/operations/finish']").exists())
                .andExpect(jsonPath("$.paths['/api/operations/recalculate']").exists());
    }

    @Test
    void unsupportedDateIsRejected() throws Exception {
        mockMvc.perform(post("/api/participants/initialize")
                        .queryParam("eventDate", "2026-09-23"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EVENT_DATE_INVALID"));
    }

    private String initialize(LocalDate eventDate) throws Exception {
        return tokenFrom(mockMvc.perform(post("/api/participants/initialize")
                        .queryParam("eventDate", eventDate.toString()))
                .andExpect(status().isOk())
                .andReturn());
    }

    private ResultActions createEntry(
            String token,
            LocalDate eventDate,
            String numbers,
            String phoneNumber,
            boolean privacyAgreed,
            int expectedStatus) throws Exception {
        String body = "{\"eventDate\":\"" + eventDate
                + "\",\"numbers\":" + numbers
                + ",\"phoneNumber\":\"" + phoneNumber
                + "\",\"privacyAgreed\":" + privacyAgreed + "}";
        return mockMvc.perform(post("/api/game/entries")
                        .cookie(cookie(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    private ResultActions submitNumber(
            String token,
            LocalDate eventDate,
            int number,
            int expectedStatus) throws Exception {
        String body = "{\"eventDate\":\"" + eventDate + "\",\"number\":" + number + "}";
        return mockMvc.perform(post("/api/game/submissions")
                        .cookie(cookie(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(expectedStatus));
    }

    private ResultActions executeBonus(LocalDate eventDate, int expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/operations/bonus")
                        .queryParam("eventDate", eventDate.toString()))
                .andExpect(status().is(expectedStatus));
    }

    private Cookie cookie(String token) {
        return new Cookie("participant_token", token);
    }

    private String tokenFrom(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        return setCookie.substring("participant_token=".length(), setCookie.indexOf(';'));
    }
}
