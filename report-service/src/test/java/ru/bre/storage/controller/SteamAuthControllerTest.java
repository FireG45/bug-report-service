package ru.bre.storage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.bre.storage.dto.SteamAuthResponse;
import ru.bre.storage.exception.PlaytestAccessDeniedException;
import ru.bre.storage.exception.SteamTicketRejectedException;
import ru.bre.storage.exception.SteamUnavailableException;
import ru.bre.storage.service.SteamAuthService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SteamAuthControllerTest {

    private SteamAuthService steamAuthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        steamAuthService = mock(SteamAuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SteamAuthController(steamAuthService))
                .build();
    }

    @Test
    void testIssueTokenForValidTicket() throws Exception {
        when(steamAuthService.issueToken("deadbeef00112233"))
                .thenReturn(new SteamAuthResponse("issued-token", 3600));

        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("issued-token"))
                .andExpect(jsonPath("$.expires_in").value(3600));
    }

    @Test
    void testRejectNonHexTicket() throws Exception {
        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"not-a-hex-ticket"}
                                """))
                .andExpect(status().isBadRequest());

        verify(steamAuthService, never()).issueToken(anyString());
    }

    @Test
    void testRejectMissingTicket() throws Exception {
        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRejectTicketRejectedBySteam() throws Exception {
        doThrow(new SteamTicketRejectedException()).when(steamAuthService).issueToken(anyString());

        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRejectWithoutPlaytestLicense() throws Exception {
        doThrow(new PlaytestAccessDeniedException()).when(steamAuthService).issueToken(anyString());

        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUnavailableWhenSteamFails() throws Exception {
        doThrow(new SteamUnavailableException()).when(steamAuthService).issueToken(anyString());

        mockMvc.perform(post("/telemetry/auth/steam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticket":"deadbeef00112233"}
                                """))
                .andExpect(status().isServiceUnavailable());
    }
}
