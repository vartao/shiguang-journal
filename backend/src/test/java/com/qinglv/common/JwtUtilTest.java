package com.qinglv.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 生成与校验基础测试
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        setField(jwtUtil, "secret", "qinglv-note-jwt-secret-key-2024-love-forever-5201314");
        setField(jwtUtil, "expiration", 3600_000L);
    }

    @Test
    void generateAndValidateToken() {
        String token = jwtUtil.generateToken(1L, "alice");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(1L, jwtUtil.getUserIdFromToken(token));
        assertEquals("alice", jwtUtil.getUsernameFromToken(token));
        assertFalse(jwtUtil.isTokenExpired(token));
    }

    @Test
    void invalidToken_shouldFailValidation() {
        assertFalse(jwtUtil.validateToken("not.a.jwt"));
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void expiredToken_shouldBeInvalid() throws Exception {
        setField(jwtUtil, "expiration", 1L);
        String token = jwtUtil.generateToken(2L, "bob");
        Thread.sleep(20);
        assertFalse(jwtUtil.validateToken(token));
        assertTrue(jwtUtil.isTokenExpired(token));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
