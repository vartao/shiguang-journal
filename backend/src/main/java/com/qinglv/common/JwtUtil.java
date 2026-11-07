package com.qinglv.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类 - 生成、验证、解析 Token
 * <p>
 * 核心功能：
 * - 生成带用户ID和用户名的JWT
 * - Token 解析（userId / username）
 * - 安全验证（防篡改、防过期）
 * - 生产环境安全警告（默认密钥）
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @PostConstruct
    public void init() {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters. Set JWT_SECRET environment variable.");
        }
        if (secret.contains("dev-only")) {
            log.warn("===== WARNING: Using default JWT secret! Set JWT_SECRET env variable in production! =====");
        }
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return 生成的 JWT 字符串
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration);

        log.debug("生成新 Token - 用户ID: {}", userId);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析 Token 获取 Claims
     *
     * @param token JWT token
     * @return Claims 对象
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     *
     * @param token JWT token
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return Long.parseLong(userId.toString());
    }

    /**
     * 从 Token 中获取用户名
     *
     * @param token JWT token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("username", String.class);
    }

    /**
     * 验证 Token 是否有效（含过期校验）
     *
     * @param token JWT token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            log.debug("Token 验证通过");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期");
            return false;
        } catch (Exception e) {
            log.warn("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 校验 Token 并一次性解析出用户 ID。
     * <p>
     * 供拦截器使用，避免 {@link #validateToken(String)} 与
     * {@link #getUserIdFromToken(String)} 各解析一次造成重复签名校验。
     *
     * @param token JWT token
     * @return 用户 ID；Token 无效或已过期时返回 {@code null}
     */
    public Long getValidUserId(String token) {
        try {
            Object userId = parseToken(token).get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            }
            return userId != null ? Long.parseLong(userId.toString()) : null;
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期");
            return null;
        } catch (Exception e) {
            log.warn("Token 验证失败: {}", e.getMessage());
            return null;
        }
    }
}
