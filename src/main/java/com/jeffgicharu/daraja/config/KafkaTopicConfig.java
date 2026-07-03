package com.jeffgicharu.daraja.config;

import com.jeffgicharu.daraja.events.OutboxRelay;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic paymentsEventsTopic() {
        return TopicBuilder.name(OutboxRelay.TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
