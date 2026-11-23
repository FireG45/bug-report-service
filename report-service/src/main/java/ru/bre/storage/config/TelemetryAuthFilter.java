package ru.bre.storage.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.bre.storage.service.SteamAuthService;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(2)
public class TelemetryAuthFilter extends OncePerRequestFilter {

    public static final String PLAYER_ID_ATTRIBUTE = "telemetry.playerId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final SteamAuthService steamAuthService;
    private final int accountRateLimitPerMinute;
    private final Cache<UUID, Bucket> accountBuckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public TelemetryAuthFilter(SteamAuthService steamAuthService,
                               @Value("${telemetry.account-rate-limit-per-minute:100}") int accountRateLimitPerMinute) {
        this.steamAuthService = steamAuthService;
        this.accountRateLimitPerMinute = accountRateLimitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/telemetry/batches");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        Optional<UUID> playerId = steamAuthService.resolvePlayerId(extractToken(request));
        if (playerId.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        Bucket bucket = accountBuckets.get(playerId.get(), id -> newBucket(accountRateLimitPerMinute));
        if (!bucket.tryConsume(1)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(bucket)));
            response.getWriter().write("Too many requests. Try again later.");
            return;
        }
        request.setAttribute(PLAYER_ID_ATTRIBUTE, playerId.get());
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith(BEARER_PREFIX)
                ? header.substring(BEARER_PREFIX.length()).trim()
                : null;
    }

    private Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(perMinute, Refill.greedy(perMinute, Duration.ofMinutes(1))))
                .build();
    }

    private long retryAfterSeconds(Bucket bucket) {
        long nanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        return Math.max(1, (nanos + 999_999_999) / 1_000_000_000);
    }
}
