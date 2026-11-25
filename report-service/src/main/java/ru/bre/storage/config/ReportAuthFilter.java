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
public class ReportAuthFilter extends OncePerRequestFilter {

    public static final String PLAYER_ID_ATTRIBUTE = "report.playerId";

    private static final String BEARER_PREFIX = "Bearer ";

    private final SteamAuthService steamAuthService;
    private final boolean requireAuth;
    private final int accountRateLimitPerHour;
    private final Cache<UUID, Bucket> accountBuckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    public ReportAuthFilter(SteamAuthService steamAuthService,
                            @Value("${report.require-auth:true}") boolean requireAuth,
                            @Value("${report.account-rate-limit-per-hour:40}") int accountRateLimitPerHour) {
        this.steamAuthService = steamAuthService;
        this.requireAuth = requireAuth;
        this.accountRateLimitPerHour = accountRateLimitPerHour;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/report-send".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            if (requireAuth) {
                reject(response, HttpStatus.UNAUTHORIZED, "Steam authentication required.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        Optional<UUID> playerId = steamAuthService.resolvePlayerId(extractToken(header));
        if (playerId.isEmpty()) {
            reject(response, HttpStatus.UNAUTHORIZED, "Invalid or expired Steam token.");
            return;
        }
        Bucket bucket = accountBuckets.get(playerId.get(), id -> newBucket(accountRateLimitPerHour));
        if (!bucket.tryConsume(1)) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(bucket)));
            reject(response, HttpStatus.TOO_MANY_REQUESTS, "Report limit exceeded. Try again later.");
            return;
        }
        request.setAttribute(PLAYER_ID_ATTRIBUTE, playerId.get());
        filterChain.doFilter(request, response);
    }

    private String extractToken(String header) {
        return header.startsWith(BEARER_PREFIX) ? header.substring(BEARER_PREFIX.length()).trim() : null;
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        response.setStatus(status.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    private Bucket newBucket(int perHour) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(perHour, Refill.greedy(perHour, Duration.ofHours(1))))
                .build();
    }

    private long retryAfterSeconds(Bucket bucket) {
        long nanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        return Math.max(1, (nanos + 999_999_999) / 1_000_000_000);
    }
}
