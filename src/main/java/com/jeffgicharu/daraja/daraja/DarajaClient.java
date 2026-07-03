package com.jeffgicharu.daraja.daraja;

import com.jeffgicharu.daraja.config.DarajaProperties;
import com.jeffgicharu.daraja.daraja.dto.StkPushRequest;
import com.jeffgicharu.daraja.daraja.dto.StkPushResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the Daraja STK push ("Lipa na M-Pesa Online") API. Builds the
 * timestamped password, attaches the bearer token, and posts the request.
 */
@Component
public class DarajaClient {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TRANSACTION_TYPE = "CustomerPayBillOnline";

    private final RestClient restClient;
    private final DarajaTokenProvider tokenProvider;
    private final DarajaProperties properties;
    private final Clock clock;

    public DarajaClient(RestClient darajaRestClient, DarajaTokenProvider tokenProvider,
                        DarajaProperties properties, Clock clock) {
        this.restClient = darajaRestClient;
        this.tokenProvider = tokenProvider;
        this.properties = properties;
        this.clock = clock;
    }

    public StkPushResponse initiateStkPush(String phoneNumber, BigDecimal amount, String accountReference,
                                           String description) {
        String timestamp = TIMESTAMP_FORMAT.format(clock.instant().atZone(ZoneId.of("Africa/Nairobi")));
        StkPushRequest request = new StkPushRequest(
                properties.shortcode(),
                buildPassword(timestamp),
                timestamp,
                TRANSACTION_TYPE,
                amount,
                phoneNumber,
                properties.shortcode(),
                phoneNumber,
                properties.callbackBaseUrl() + "/api/v1/payments/callback",
                accountReference,
                description
        );

        return restClient.post()
                .uri("/mpesa/stkpush/v1/processrequest")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(StkPushResponse.class);
    }

    private String buildPassword(String timestamp) {
        String raw = properties.shortcode() + properties.passkey() + timestamp;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
