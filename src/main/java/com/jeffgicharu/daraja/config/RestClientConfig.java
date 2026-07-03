package com.jeffgicharu.daraja.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /** RestClient pointed at the Daraja base URL, shared by the token provider and API client. */
    @Bean
    RestClient darajaRestClient(DarajaProperties properties) {
        return RestClient.builder().baseUrl(properties.baseUrl()).build();
    }
}
