package com.jeffgicharu.daraja.daraja;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.jeffgicharu.daraja.config.DarajaProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DarajaTokenProviderTest {

    private static final String BASE_URL = "https://sandbox.example.co.ke";

    private MutableClock clock;
    private MockRestServiceServer server;
    private DarajaTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        DarajaProperties properties = new DarajaProperties(
                BASE_URL, "the-key", "the-secret", "174379", "passkey", "https://cb.example");
        clock = new MutableClock(Instant.parse("2026-07-03T20:00:00Z"));
        tokenProvider = new DarajaTokenProvider(restClient, properties, clock);
    }

    @Test
    void fetchesTokenWithBasicAuthHeader() {
        String expectedBasic = "Basic "
                + Base64.getEncoder().encodeToString("the-key:the-secret".getBytes());
        server.expect(requestTo(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", expectedBasic))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-123\",\"expires_in\":\"3599\"}",
                        MediaType.APPLICATION_JSON));

        assertThat(tokenProvider.getAccessToken()).isEqualTo("tok-123");
        server.verify();
    }

    @Test
    void cachesTokenAndDoesNotRefetchWithinValidityWindow() {
        server.expect(requestTo(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials"))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-cached\",\"expires_in\":\"3599\"}",
                        MediaType.APPLICATION_JSON));

        String first = tokenProvider.getAccessToken();
        clock.advance(Duration.ofSeconds(1000)); // still well within the 3599s validity
        String second = tokenProvider.getAccessToken();

        assertThat(first).isEqualTo("tok-cached");
        assertThat(second).isEqualTo("tok-cached");
        server.verify(); // exactly one HTTP call was expected
    }

    @Test
    void refreshesTokenAfterExpiryWindow() {
        server.expect(requestTo(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials"))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-old\",\"expires_in\":\"3599\"}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials"))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-new\",\"expires_in\":\"3599\"}",
                        MediaType.APPLICATION_JSON));

        String first = tokenProvider.getAccessToken();
        clock.advance(Duration.ofSeconds(3599)); // past the safety window
        String second = tokenProvider.getAccessToken();

        assertThat(first).isEqualTo("tok-old");
        assertThat(second).isEqualTo("tok-new");
        server.verify();
    }

    @Test
    void throwsWhenTokenResponseIsEmpty() {
        server.expect(requestTo(BASE_URL + "/oauth/v1/generate?grant_type=client_credentials"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> tokenProvider.getAccessToken())
                .isInstanceOf(DarajaAuthenticationException.class);
    }

    /** A Clock whose instant can be advanced within a test. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
