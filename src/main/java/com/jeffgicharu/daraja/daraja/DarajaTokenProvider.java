package com.jeffgicharu.daraja.daraja;

import com.jeffgicharu.daraja.config.DarajaProperties;
import com.jeffgicharu.daraja.daraja.dto.TokenResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches and caches the Daraja OAuth access token. Daraja tokens are valid for
 * ~3599 seconds; we reuse a cached token until it is within a safety window of
 * expiry, then transparently refresh. Thread-safe.
 */
@Component
public class DarajaTokenProvider {

    /** Refresh a little before the real expiry to avoid using a token that dies mid-request. */
    private static final Duration EXPIRY_SAFETY_WINDOW = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final DarajaProperties properties;
    private final Clock clock;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.MIN;

    public DarajaTokenProvider(RestClient darajaRestClient, DarajaProperties properties, Clock clock) {
        this.restClient = darajaRestClient;
        this.properties = properties;
        this.clock = clock;
    }

    public String getAccessToken() {
        Instant now = clock.instant();
        String token = cachedToken;
        if (token != null && now.isBefore(expiresAt.minus(EXPIRY_SAFETY_WINDOW))) {
            return token;
        }
        synchronized (this) {
            now = clock.instant();
            if (cachedToken != null && now.isBefore(expiresAt.minus(EXPIRY_SAFETY_WINDOW))) {
                return cachedToken;
            }
            return refreshToken(now);
        }
    }

    private String refreshToken(Instant now) {
        TokenResponse response = restClient.get()
                .uri("/oauth/v1/generate?grant_type=client_credentials")
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader())
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new DarajaAuthenticationException("Daraja returned an empty OAuth token response");
        }
        long expiresInSeconds = parseExpiresIn(response.expiresIn());
        this.cachedToken = response.accessToken();
        this.expiresAt = now.plusSeconds(expiresInSeconds);
        return this.cachedToken;
    }

    private String basicAuthHeader() {
        String credentials = properties.consumerKey() + ":" + properties.consumerSecret();
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private long parseExpiresIn(String expiresIn) {
        try {
            return Long.parseLong(expiresIn.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return 3599L;
        }
    }
}
