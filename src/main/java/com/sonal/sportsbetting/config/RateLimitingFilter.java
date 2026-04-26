package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, ClientWindow> windows = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = request.getRemoteAddr() + ":" + request.getRequestURI();
        long nowEpochSecond = Instant.now().getEpochSecond();

        ClientWindow window = windows.compute(clientKey, (key, existing) -> {
            if (existing == null || nowEpochSecond - existing.windowStartEpochSecond() >= properties.getWindowSeconds()) {
                return new ClientWindow(nowEpochSecond, new AtomicInteger(1));
            }
            existing.counter().incrementAndGet();
            return existing;
        });

        if (window.counter().get() > properties.getRequests()) {
            throw new RateLimitExceededException("Too many requests. Please retry later.");
        }

        filterChain.doFilter(request, response);
    }

    private record ClientWindow(long windowStartEpochSecond, AtomicInteger counter) {
    }
}
