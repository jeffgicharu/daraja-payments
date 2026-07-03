package com.jeffgicharu.daraja;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Full-stack integration test: real Spring context, real MySQL and Kafka
 * (Testcontainers), and a WireMock stand-in for the Daraja API. Exercises the
 * complete flow: token issuance -> authenticated STK push -> Daraja callback ->
 * state transition -> outbox relay -> Kafka events.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentFlowIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    static WireMockServer daraja = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    static final String CHECKOUT_ID = "it-checkout-0001";

    @BeforeAll
    static void startWireMock() {
        daraja.start();
        daraja.stubFor(get(urlPathEqualTo("/oauth/v1/generate")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"access_token\":\"it-token\",\"expires_in\":\"3599\"}")));
        daraja.stubFor(post(urlPathEqualTo("/mpesa/stkpush/v1/processrequest")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"MerchantRequestID\":\"it-merchant\",\"CheckoutRequestID\":\"" + CHECKOUT_ID
                        + "\",\"ResponseCode\":\"0\",\"ResponseDescription\":\"Success\","
                        + "\"CustomerMessage\":\"Enter PIN\"}")));
    }

    @AfterAll
    static void stopWireMock() {
        daraja.stop();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("daraja.base-url", () -> daraja.baseUrl());
        registry.add("daraja.consumer-key", () -> "it-key");
        registry.add("daraja.consumer-secret", () -> "it-secret");
        registry.add("daraja.passkey", () -> "it-passkey");
        registry.add("daraja.callback-base-url", () -> "https://it.example.com");
        registry.add("outbox.relay-interval-ms", () -> "500");
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (request, response) -> {
                })
                .build();
    }

    private String obtainAccessToken() {
        ResponseEntity<String> response = client().post()
                .uri("/api/v1/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("clientId", "demo-client", "clientSecret", "demo-secret"))
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return objectMapper.readTree(response.getBody()).get("accessToken").asString();
    }

    @Test
    @Order(1)
    void paymentWithoutTokenIsRejected() {
        ResponseEntity<String> response = client().post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("phoneNumber", "254712345678", "amount", 10, "accountReference", "IT-1"))
                .retrieve()
                .toEntity(String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Order(2)
    void fullPaymentLifecycleEmitsKafkaEvents() {
        String token = obtainAccessToken();

        // 1. Authenticated STK push (Daraja stubbed by WireMock)
        ResponseEntity<String> initiate = client().post()
                .uri("/api/v1/payments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("phoneNumber", "254712345678", "amount", 10, "accountReference", "IT-1"))
                .retrieve()
                .toEntity(String.class);
        assertThat(initiate.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(objectMapper.readTree(initiate.getBody()).get("checkoutRequestId").asString())
                .isEqualTo(CHECKOUT_ID);

        // 2. Daraja result callback (public webhook)
        String callback = """
                {"Body":{"stkCallback":{"MerchantRequestID":"it-merchant","CheckoutRequestID":"%s",
                "ResultCode":0,"ResultDesc":"Success","CallbackMetadata":{"Item":[
                {"Name":"Amount","Value":10},{"Name":"MpesaReceiptNumber","Value":"ITRECEIPT1"}]}}}}
                """.formatted(CHECKOUT_ID);
        ResponseEntity<String> cb = client().post()
                .uri("/api/v1/payments/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .body(callback)
                .retrieve()
                .toEntity(String.class);
        assertThat(cb.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 3. Transaction is COMPLETED with the receipt
        ResponseEntity<String> get = client().get()
                .uri("/api/v1/payments/" + CHECKOUT_ID)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .toEntity(String.class);
        assertThat(get.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode tx = objectMapper.readTree(get.getBody());
        assertThat(tx.get("status").asString()).isEqualTo("COMPLETED");
        assertThat(tx.get("mpesaReceipt").asString()).isEqualTo("ITRECEIPT1");

        // 4. The outbox relay publishes both lifecycle events to Kafka
        List<String> events = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of("payments.events"));
            await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    events.add(objectMapper.readTree(record.value()).get("eventType").asString());
                }
                assertThat(events).contains("payment.initiated", "payment.completed");
            });
        }
    }

    private KafkaConsumer<String, String> testConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        props.put("group.id", "it-assertions");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", StringDeserializer.class.getName());
        return new KafkaConsumer<>(props);
    }
}
