package ru.bre.storage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.bre.storage.config.SteamAuthProperties;
import ru.bre.storage.dto.SteamAuthResponse;
import ru.bre.storage.exception.PlaytestAccessDeniedException;
import ru.bre.storage.exception.SteamTicketRejectedException;
import ru.bre.storage.exception.SteamUnavailableException;
import ru.bre.storage.model.AuthToken;
import ru.bre.storage.repository.AuthTokenRepository;
import ru.bre.storage.repository.PlayerRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SteamAuthServiceTest {

    private static final long STEAM_ID = 76561198000000000L;
    private static final UUID PLAYER_ID = UUID.fromString("9f1f6c1e-4d4e-4d43-9d20-6a2f1f0a4b7c");

    @Mock
    private SteamWebApiClient steamClient;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    private SteamAuthService steamAuthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        steamAuthService = new SteamAuthService(
                steamClient, playerRepository, authTokenRepository, new SteamAuthProperties());
    }

    @Test
    void testIssueTokenForLicensedAccount() {
        when(steamClient.authenticateTicket("ticket")).thenReturn(STEAM_ID);
        when(steamClient.ownsApp(STEAM_ID)).thenReturn(true);
        when(steamClient.fetchPlayerName(STEAM_ID)).thenReturn(Optional.of("Player"));
        when(playerRepository.findPlayerIdBySteamId64(STEAM_ID)).thenReturn(PLAYER_ID);

        SteamAuthResponse response = steamAuthService.issueToken("ticket");

        assertEquals(3600, response.getExpiresIn());
        assertEquals(64, response.getAccessToken().length());
        verify(playerRepository).upsert(any(UUID.class), eq(STEAM_ID), eq("Player"));

        ArgumentCaptor<AuthToken> tokenCaptor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenRepository).save(tokenCaptor.capture());
        AuthToken saved = tokenCaptor.getValue();
        assertEquals(PLAYER_ID, saved.getPlayerId());
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(response.getAccessToken(), saved.getTokenHash());
        assertTrue(saved.getExpiresAt().isAfter(saved.getIssuedAt()));
    }

    @Test
    void testSameSteamAccountMapsToSamePlayer() {
        when(steamClient.authenticateTicket("ticket")).thenReturn(STEAM_ID);
        when(steamClient.ownsApp(STEAM_ID)).thenReturn(true);
        when(steamClient.fetchPlayerName(STEAM_ID)).thenReturn(Optional.empty());
        when(playerRepository.findPlayerIdBySteamId64(STEAM_ID)).thenReturn(PLAYER_ID);

        steamAuthService.issueToken("ticket");
        steamAuthService.issueToken("ticket");

        ArgumentCaptor<AuthToken> tokenCaptor = ArgumentCaptor.forClass(AuthToken.class);
        verify(authTokenRepository, times(2)).save(tokenCaptor.capture());
        assertTrue(tokenCaptor.getAllValues().stream()
                .allMatch(token -> PLAYER_ID.equals(token.getPlayerId())));
        verify(playerRepository, times(2)).upsert(any(UUID.class), eq(STEAM_ID), isNull());
    }

    @Test
    void testPropagateRejectedTicket() {
        when(steamClient.authenticateTicket("ticket")).thenThrow(new SteamTicketRejectedException());

        assertThrows(SteamTicketRejectedException.class, () -> steamAuthService.issueToken("ticket"));

        verifyNoInteractions(playerRepository);
        verify(authTokenRepository, never()).save(any());
    }

    @Test
    void testDenyWithoutPlaytestLicense() {
        when(steamClient.authenticateTicket("ticket")).thenReturn(STEAM_ID);
        when(steamClient.ownsApp(STEAM_ID)).thenReturn(false);

        assertThrows(PlaytestAccessDeniedException.class, () -> steamAuthService.issueToken("ticket"));

        verifyNoInteractions(playerRepository);
        verify(authTokenRepository, never()).save(any());
    }

    @Test
    void testPropagateSteamUnavailable() {
        when(steamClient.authenticateTicket("ticket")).thenThrow(new SteamUnavailableException());

        assertThrows(SteamUnavailableException.class, () -> steamAuthService.issueToken("ticket"));

        verify(authTokenRepository, never()).save(any());
    }

    @Test
    void testResolveActiveTokenByHash() {
        when(authTokenRepository.findActivePlayerId(anyString())).thenReturn(Optional.of(PLAYER_ID));

        assertEquals(Optional.of(PLAYER_ID), steamAuthService.resolvePlayerId("some-token"));

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(authTokenRepository).findActivePlayerId(hashCaptor.capture());
        assertEquals(64, hashCaptor.getValue().length());
        assertNotEquals("some-token", hashCaptor.getValue());
    }

    @Test
    void testResolveEmptyToken() {
        assertEquals(Optional.empty(), steamAuthService.resolvePlayerId(null));
        assertEquals(Optional.empty(), steamAuthService.resolvePlayerId(""));

        verifyNoInteractions(authTokenRepository);
    }

    @Test
    void testPurgeExpiredTokens() {
        when(authTokenRepository.deleteExpired()).thenReturn(3);

        steamAuthService.purgeExpiredTokens();

        verify(authTokenRepository).deleteExpired();
    }
}
