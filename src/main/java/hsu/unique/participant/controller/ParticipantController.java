package hsu.unique.participant.controller;

import hsu.unique.participant.dto.InitializeParticipantResponse;
import hsu.unique.participant.dto.ParticipantMeResponse;
import hsu.unique.participant.service.ParticipantCookieManager;
import hsu.unique.participant.service.ParticipantInitialization;
import hsu.unique.participant.service.ParticipantService;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantCookieManager cookieManager;

    public ParticipantController(
            ParticipantService participantService,
            ParticipantCookieManager cookieManager) {
        this.participantService = participantService;
        this.cookieManager = cookieManager;
    }

    @PostMapping("/initialize")
    public ResponseEntity<InitializeParticipantResponse> initialize(
            @CookieValue(name = ParticipantCookieManager.COOKIE_NAME, required = false) String cookieToken,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {
        ParticipantInitialization initialization = participantService.initialize(cookieToken, eventDate);
        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (initialization.cookieIssued()) {
            response.header(HttpHeaders.SET_COOKIE,
                    cookieManager.create(initialization.participant().getParticipantToken()).toString());
        }
        return response.body(InitializeParticipantResponse.from(initialization.dailyParticipation()));
    }

    @GetMapping("/me")
    public ParticipantMeResponse getMe(
            @CookieValue(name = ParticipantCookieManager.COOKIE_NAME, required = false) String cookieToken,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {
        return participantService.getMe(cookieToken, eventDate);
    }
}
