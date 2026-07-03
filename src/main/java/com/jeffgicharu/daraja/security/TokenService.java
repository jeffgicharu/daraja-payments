package com.jeffgicharu.daraja.security;

import com.jeffgicharu.daraja.config.ApiSecurityProperties;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived HS256 access tokens for API clients. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final ApiSecurityProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, ApiSecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issueFor(String clientId) {
        Instant now = clock.instant();
        long ttl = properties.tokenTtlSeconds();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("daraja-payments")
                .subject(clientId)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttl))
                .claim("scope", "payments")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, "Bearer", ttl);
    }

    public boolean validClient(String clientId, String clientSecret) {
        return properties.clientId().equals(clientId)
                && properties.clientSecret().equals(clientSecret);
    }

    public record IssuedToken(String accessToken, String tokenType, long expiresInSeconds) {
    }
}
