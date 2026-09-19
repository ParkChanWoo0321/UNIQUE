package hsu.unique.participant.service;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class ParticipantCookieManager {

    public static final String COOKIE_NAME = "participant_token";

    private final boolean secure;
    private final String sameSite;
    private final long maxAgeDays;

    public ParticipantCookieManager(
            @Value("${app.participant-cookie.secure:false}") boolean secure,
            @Value("${app.participant-cookie.same-site:Lax}") String sameSite,
            @Value("${app.participant-cookie.max-age-days:30}") long maxAgeDays) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeDays = maxAgeDays;
    }

    public ResponseCookie create(String participantToken) {
        return ResponseCookie.from(COOKIE_NAME, participantToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofDays(maxAgeDays))
                .build();
    }
}
