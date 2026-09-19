package hsu.unique.game.controller;

import hsu.unique.game.dto.CreateEntryRequest;
import hsu.unique.game.dto.CreateEntryResponse;
import hsu.unique.game.dto.GameStatusResponse;
import hsu.unique.game.dto.SubmissionResponse;
import hsu.unique.game.dto.SubmitNumberRequest;
import hsu.unique.game.service.GameService;
import hsu.unique.participant.service.ParticipantCookieManager;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping("/entries")
    public CreateEntryResponse createEntry(
            @CookieValue(name = ParticipantCookieManager.COOKIE_NAME, required = false) String cookieToken,
            @Valid @RequestBody CreateEntryRequest request) {
        return gameService.createEntry(cookieToken, request);
    }

    @PostMapping("/submissions")
    public SubmissionResponse submit(
            @CookieValue(name = ParticipantCookieManager.COOKIE_NAME, required = false) String cookieToken,
            @Valid @RequestBody SubmitNumberRequest request) {
        return gameService.submitBonusNumber(cookieToken, request.eventDate(), request.number());
    }

    @GetMapping("/result")
    public Object getResult(
            @CookieValue(name = ParticipantCookieManager.COOKIE_NAME, required = false) String cookieToken,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {
        return gameService.getResult(cookieToken, eventDate);
    }

    @GetMapping("/status")
    public GameStatusResponse getStatus(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate) {
        return gameService.getStatus(eventDate);
    }
}
