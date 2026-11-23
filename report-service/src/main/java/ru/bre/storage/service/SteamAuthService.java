package ru.bre.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.bre.storage.config.SteamAuthProperties;
import ru.bre.storage.dto.SteamAuthResponse;
import ru.bre.storage.exception.PlaytestAccessDeniedException;
import ru.bre.storage.model.AuthToken;
import ru.bre.storage.repository.AuthTokenRepository;
import ru.bre.storage.repository.PlayerRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Обмен Steam-билета на внутренний токен. SteamID64 сопоставляется с player_id;
 * в БД хранится только SHA-256 хеш токена.
 */
@Service
public class SteamAuthService {

    private static final Logger log = LoggerFactory.getLogger(SteamAuthService.class);
    private static final HexFormat HEX = HexFormat.of();
    private static final int TOKEN_BYTES = 32;

    private final SteamWebApiClient steamClient;
    private final PlayerRepository playerRepository;
    private final AuthTokenRepository authTokenRepository;
    private final SteamAuthProperties properties;
    private final SecureRandom random = new SecureRandom();

    public SteamAuthService(SteamWebApiClient steamClient,
                            PlayerRepository playerRepository,
                            AuthTokenRepository authTokenRepository,
                            SteamAuthProperties properties) {
        this.steamClient = steamClient;
        this.playerRepository = playerRepository;
        this.authTokenRepository = authTokenRepository;
        this.properties = properties;
    }

    public SteamAuthResponse issueToken(String ticket) {
        long steamId = steamClient.authenticateTicket(ticket);
        if (!steamClient.ownsApp(steamId)) {
            throw new PlaytestAccessDeniedException();
        }
        String playerName = steamClient.fetchPlayerName(steamId).orElse(null);
        UUID playerId = resolvePlayer(steamId, playerName);

        String token = newToken();
        Instant now = Instant.now();
        AuthToken authToken = new AuthToken();
        authToken.setTokenHash(hash(token));
        authToken.setPlayerId(playerId);
        authToken.setIssuedAt(now);
        authToken.setExpiresAt(now.plus(properties.getTokenTtl()));
        authTokenRepository.save(authToken);

        log.info("Steam auth succeeded for player {}", playerId);
        return new SteamAuthResponse(token, properties.getTokenTtl().toSeconds());
    }

    public Optional<UUID> resolvePlayerId(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        return authTokenRepository.findActivePlayerId(hash(token));
    }

    @Scheduled(cron = "0 17 4 * * *")
    public void purgeExpiredTokens() {
        int deleted = authTokenRepository.deleteExpired();
        if (deleted > 0) {
            log.info("Purged {} expired auth tokens", deleted);
        }
    }

    private UUID resolvePlayer(long steamId, String playerName) {
        playerRepository.upsert(UUID.randomUUID(), steamId, playerName);
        return playerRepository.findPlayerIdBySteamId64(steamId);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
