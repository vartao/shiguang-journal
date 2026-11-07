package com.qinglv.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtInterceptorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesValidatedUserIdOnRequest() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtUtil.getValidUserId("token")).thenReturn(7L);

        JwtInterceptor interceptor = new JwtInterceptor(jwtUtil, objectMapper);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        verify(request).setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, 7L);
    }
}
