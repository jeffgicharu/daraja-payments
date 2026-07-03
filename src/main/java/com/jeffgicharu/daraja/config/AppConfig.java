package com.jeffgicharu.daraja.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** A single injectable Clock so time-dependent logic (token expiry, timestamps) is testable. */
    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
