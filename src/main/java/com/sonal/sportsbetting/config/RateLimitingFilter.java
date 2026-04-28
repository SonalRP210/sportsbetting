package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {
    private final RateLimiterGateway rateLimiterGateway;

    public RateLimitingFilter(RateLimiterGateway rateLimiterGateway) {
        this.rateLimiterGateway = rateLimiterGateway;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientKey = request.getRemoteAddr() + ":" + request.getRequestURI();
        boolean accepted = rateLimiterGateway.tryConsume(clientKey);
        if (!accepted) {
            throw new RateLimitExceededException("Too many requests. Please retry later.");
        }

        filterChain.doFilter(request, response);
    }
}
