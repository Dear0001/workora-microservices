package com.workora.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InMemoryRateLimitFilter implements GlobalFilter {

    private final int limit;
    private final long windowMillis;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimitFilter(
            @Value("${gateway.rate-limit.requests-per-window:120}") int limit,
            @Value("${gateway.rate-limit.window-seconds:60}") long windowSeconds
    ) {
        if (limit < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("Rate-limit values must be positive");
        }
        this.limit = limit;
        this.windowMillis = windowSeconds * 1000;
        this.clock = Clock.systemUTC();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/api/") || path.endsWith("/health")) {
            return chain.filter(exchange);
        }

        String client = exchange.getRequest().getRemoteAddress() == null
                ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        long now = Instant.now(clock).toEpochMilli();
        Window window = windows.compute(client, (key, current) -> {
            if (current == null || now - current.startedAt >= windowMillis) {
                return new Window(now);
            }
            current.count.incrementAndGet();
            return current;
        });

        if (window.count.get() > limit) {
            var response = exchange.getResponse();
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            response.getHeaders().set("Retry-After", Long.toString(Math.max(1, (windowMillis - (now - window.startedAt)) / 1000)));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(
                    "{\"success\":false,\"message\":\"Rate limit exceeded\"}".getBytes())));
        }
        return chain.filter(exchange);
    }

    private static final class Window {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}
