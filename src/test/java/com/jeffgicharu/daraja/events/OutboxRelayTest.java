package com.jeffgicharu.daraja.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jeffgicharu.daraja.domain.OutboxEvent;
import com.jeffgicharu.daraja.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository repository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay relay;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(Instant.parse("2026-07-04T00:00:00Z"), ZoneOffset.UTC);
        relay = new OutboxRelay(repository, kafkaTemplate, fixed);
    }

    @Test
    void publishesPendingEventsAndMarksThemPublished() {
        OutboxEvent event = new OutboxEvent("payment", "c-1", "payment.initiated", "{}");
        when(repository.findByPublishedAtIsNullOrderByIdAsc(any(Limit.class)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mockSendResult()));

        relay.relayPendingEvents();

        assertThat(event.getPublishedAt()).isEqualTo(Instant.parse("2026-07-04T00:00:00Z"));
        verify(kafkaTemplate).send(OutboxRelay.TOPIC, "c-1", "{}");
        verify(repository).save(event);
    }

    @Test
    void leavesEventUnpublishedWhenBrokerSendFails() {
        OutboxEvent first = new OutboxEvent("payment", "c-1", "payment.initiated", "{}");
        OutboxEvent second = new OutboxEvent("payment", "c-2", "payment.initiated", "{}");
        when(repository.findByPublishedAtIsNullOrderByIdAsc(any(Limit.class)))
                .thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

        relay.relayPendingEvents();

        // First send failed: nothing is marked published and the batch stops
        // (preserving order) — both rows retry on the next tick.
        assertThat(first.getPublishedAt()).isNull();
        assertThat(second.getPublishedAt()).isNull();
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
        verify(repository, times(0)).save(any());
    }

    @SuppressWarnings("unchecked")
    private SendResult<String, String> mockSendResult() {
        return (SendResult<String, String>) org.mockito.Mockito.mock(SendResult.class);
    }
}
