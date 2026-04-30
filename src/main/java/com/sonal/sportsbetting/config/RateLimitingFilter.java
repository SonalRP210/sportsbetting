package com.sonal.sportsbetting.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimiterGateway rateLimiterGateway;

    public RateLimitingFilter(RateLimiterGateway rateLimiterGateway) {
        this.rateLimiterGateway = rateLimiterGateway;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = request.getRemoteAddr() + ":" + request.getRequestURI();
        boolean accepted;
        try {
            accepted = rateLimiterGateway.tryConsume(clientKey);
        } catch (Exception ex) {
            log.warn("Rate limiter unavailable for key={}, failing open", clientKey, ex);
            filterChain.doFilter(request, response);
            return;
        }
        if (!accepted) {
            log.debug("Rate limit exceeded for key={}", clientKey);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"code\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please retry later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
