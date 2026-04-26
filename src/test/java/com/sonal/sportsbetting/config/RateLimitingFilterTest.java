package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitingFilterTest {

    @Test
    void throwsWhenRequestCountExceedsLimit() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setRequests(1);
        properties.setWindowSeconds(60);
        RateLimitingFilter filter = new RateLimitingFilter(properties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/bets");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
        assertThrows(RateLimitExceededException.class, () -> filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()));
    }
}
