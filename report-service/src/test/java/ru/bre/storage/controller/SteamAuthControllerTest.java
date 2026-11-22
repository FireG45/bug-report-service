package ru.bre.storage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bre.storage.service.SteamAuthService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SteamAuthControllerTest {

    private SteamAuthService steamAuthService;
    private MockMvc stubEnabledMvc;
    private MockMvc stubDisabledMvc;

    @BeforeEach
    void setUp() {
        steamAuthService = new SteamAuthService(true);
        stubEnabledMvc = MockMvcBuilders
                .standaloneSetup(new SteamAuthController(steamAuthService))
                .build();
        stubDisabledMvc = MockMvcBuilders
                .standaloneSetup(new SteamAuthController(new SteamAuthService(false)))
                .build();
    }

    @Test
    void testIssueTokenForValidTicket() throws Exception {
        stubEnabledMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.expires_in").value(SteamAuthService.TOKEN_TTL_SECONDS));
    }

    @Test
    void testIssuedTokenIsAccepted() throws Exception {
        String body = stubEnabledMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String token = new ObjectMapper().readTree(body).path("access_token").asText();

        assertTrue(steamAuthService.isValidToken(token));
    }

    @Test
    void testRejectNonHexTicket() throws Exception {
        stubEnabledMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"not-a-hex-ticket"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRejectMissingTicket() throws Exception {
        stubEnabledMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnavailableWhenStubDisabled() throws Exception {
        stubDisabledMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andExpect(status().isServiceUnavailable());
    }
}
