package com.jeffgicharu.daraja.web;

import com.jeffgicharu.daraja.security.TokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final TokenService tokenService;

    public AuthController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(@Valid @RequestBody TokenRequest request) {
        if (!tokenService.validClient(request.clientId(), request.clientSecret())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_client"));
        }
        return ResponseEntity.ok(tokenService.issueFor(request.clientId()));
    }

    public record TokenRequest(@NotBlank String clientId, @NotBlank String clientSecret) {
    }
}
