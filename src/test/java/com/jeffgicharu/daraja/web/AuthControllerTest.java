package com.jeffgicharu.daraja.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jeffgicharu.daraja.config.AppConfig;
import com.jeffgicharu.daraja.config.ApiSecurityProperties;
import com.jeffgicharu.daraja.config.SecurityConfig;
import com.jeffgicharu.daraja.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({AppConfig.class, SecurityConfig.class, TokenService.class})
@EnableConfigurationProperties(ApiSecurityProperties.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void issuesTokenForValidClientCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType("application/json")
                        .content("""
                                {"clientId":"demo-client","clientSecret":"demo-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    void rejectsInvalidClientCredentialsWith401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType("application/json")
                        .content("""
                                {"clientId":"demo-client","clientSecret":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsBlankCredentialsWith400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType("application/json")
                        .content("""
                                {"clientId":"","clientSecret":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
