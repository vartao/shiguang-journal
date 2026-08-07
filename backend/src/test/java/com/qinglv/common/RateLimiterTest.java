package com.qinglv.common;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Test
    void limitsAfterTenRequestsAndPrefersProxyControlledRealIp() throws Exception {
        RateLimiter limiter = new RateLimiter();

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = request("203.0.113.5", "spoofed-" + i);
            assertTrue(limiter.preHandle(request, new MockHttpServletResponse(), new Object()));
        }

        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        assertFalse(limiter.preHandle(request("203.0.113.5", "another-spoof"), blockedResponse, new Object()));
        assertEquals(429, blockedResponse.getStatus());
    }

    private MockHttpServletRequest request(String realIp, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", realIp);
        request.addHeader("X-Forwarded-For", forwardedFor);
        return request;
    }
}
