package ru.bre.storage.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.bre.storage.dto.SteamAuthResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Заглушка биржи Steam-билетов: выдаёт непрозрачный токен без проверки билета через Steam Web API.
 * Хранит только хеши выданных токенов, TTL — {@value #TOKEN_TTL_SECONDS} секунд.
 */
@Service
public class SteamAuthService {

    public static final int TOKEN_TTL_SECONDS = 3600;

    private static final Logger log = LoggerFactory.getLogger(SteamAuthService.class);
    private static final HexFormat HEX = HexFormat.of();

    private final boolean stubEnabled;
    private final SecureRandom random = new SecureRandom();
    private final Cache<String, Boolean> tokens = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(TOKEN_TTL_SECONDS))
            .maximumSize(10_000)
            .build();

    public SteamAuthService(@Value("${telemetry.steam-auth.stub-enabled:true}") boolean stubEnabled) {
        this.stubEnabled = stubEnabled;
        if (stubEnabled) {
            log.warn("Steam auth stub is enabled: tickets are accepted without Steam validation");
        }
    }

    public boolean isStubEnabled() {
        return stubEnabled;
    }

    public SteamAuthResponse issueToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HEX.formatHex(bytes);
        tokens.put(hash(token), Boolean.TRUE);
        return new SteamAuthResponse(token, TOKEN_TTL_SECONDS);
    }

    public boolean isValidToken(String token) {
        return token != null && !token.isEmpty() && tokens.getIfPresent(hash(token)) != null;
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
