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
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();

        if (path.startsWith("/healthcheck") || path.startsWith("/get-frontend-report")) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket;
        if (isBatchEndpoint(path)) {
            bucket = resolveBatchBucket(ip);
        } else if (isAuthEndpoint(path)) {
            bucket = resolveAuthBucket(ip);
        } else {
            bucket = resolveBucket(ip);
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds(bucket)));
            response.getWriter().write("Too many requests. Try again later.");
        }
    }

    private long retryAfterSeconds(Bucket bucket) {
        long nanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
        return Math.max(1, (nanos + 999_999_999) / 1_000_000_000);
    }

    private boolean isBatchEndpoint(String path) {
        return path.startsWith("/telemetry/batches");
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/telemetry/auth/steam");
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(3, Refill.intervally(1, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createBatchBucket() {
        Bandwidth limit = Bandwidth.classic(60, Refill.intervally(30, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(12, Refill.intervally(6, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket resolveBucket(String ip) {
        return cache.get(ip, k -> createNewBucket());
    }

    private Bucket resolveBatchBucket(String ip) {
        return cache.get("batch:" + ip, k -> createBatchBucket());
    }

    private Bucket resolveAuthBucket(String ip) {
        return cache.get("auth:" + ip, k -> createAuthBucket());
    }
}
