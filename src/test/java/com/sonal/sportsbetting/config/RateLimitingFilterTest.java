package com.sonal.sportsbetting.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitingFilterTest {

    @Test
    void returns429WhenRequestCountExceedsLimit() throws Exception {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequests(1);
        properties.setWindowSeconds(60);
        RateLimitingFilter filter = new RateLimitingFilter(new InMemoryRateLimiterGateway(properties));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bets");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockFilterChain firstChain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, firstResponse, firstChain));
        assertEquals(HttpStatus.OK.value(), firstResponse.getStatus());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        assertDoesNotThrow(() -> filter.doFilter(request, secondResponse, new MockFilterChain()));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), secondResponse.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, secondResponse.getContentType());
        assertTrue(secondResponse.getContentAsString().contains("RATE_LIMITED"));
    }

    @Test
    void failsOpenWhenRateLimiterThrows() {
        RateLimitingFilter filter = new RateLimitingFilter(key -> {
            throw new RuntimeException("Redis unavailable");
        });

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/odds-feed");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertEquals(HttpStatus.OK.value(), response.getStatus());
    }
}
