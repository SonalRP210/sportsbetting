package com.sonal.sportsbetting.config;

import com.sonal.sportsbetting.PropertyFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCorrelationFilterTest {

    @Test
    void setsRequestIdWhenMissing() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter(PropertyFixtures.correlation());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String headerName = PropertyFixtures.correlation().responseIdHeader();
        String header = response.getHeader(headerName);
        assertNotNull(header);
        assertTrue(header.length() > 10);
    }

    @Test
    void preservesProvidedRequestId() throws Exception {
        RequestCorrelationFilter filter = new RequestCorrelationFilter(PropertyFixtures.correlation());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.addHeader(PropertyFixtures.correlation().requestIdHeader(), "req-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertTrue("req-123".equals(response.getHeader(PropertyFixtures.correlation().responseIdHeader())));
    }
}
