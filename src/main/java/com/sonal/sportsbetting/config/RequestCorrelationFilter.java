package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.properties.CorrelationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private final CorrelationProperties correlationProperties;

    public RequestCorrelationFilter(CorrelationProperties correlationProperties) {
        this.correlationProperties = correlationProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestIdHeader = correlationProperties.requestIdHeader();
        String requestId = request.getHeader(requestIdHeader);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String mdcKey = correlationProperties.mdcKey();
        MDC.put(mdcKey, requestId);
        response.setHeader(correlationProperties.responseIdHeader(), requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(mdcKey);
        }
    }
}
