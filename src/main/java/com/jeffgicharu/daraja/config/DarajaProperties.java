package com.jeffgicharu.daraja.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Safaricom Daraja (M-Pesa) API integration.
 * Values are supplied via environment variables (see .env.example); secrets
 * are never committed.
 */
@ConfigurationProperties(prefix = "daraja")
public record DarajaProperties(
        String baseUrl,
        String consumerKey,
        String consumerSecret,
        String shortcode,
        String passkey,
        String callbackBaseUrl
) {
}
