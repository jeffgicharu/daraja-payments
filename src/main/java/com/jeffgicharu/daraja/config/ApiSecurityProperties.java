package com.jeffgicharu.daraja.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * API authentication settings. The JWT secret must be at least 32 bytes
 * (HS256). Client id/secret authenticate callers of the token endpoint.
 */
@ConfigurationProperties(prefix = "api")
public record ApiSecurityProperties(
        String jwtSecret,
        String clientId,
        String clientSecret,
        long tokenTtlSeconds
) {
}
