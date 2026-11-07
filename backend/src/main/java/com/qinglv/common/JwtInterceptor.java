package com.qinglv.common;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 拦截器
 * - 统一处理所有请求的 JWT 验证
 * - 通过 CurrentUserContext 注入当前用户上下文
 * - 记录关键安全事件（登录、过期、非法访问）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            log.debug("未提供 Authorization 头 - 可能为匿名请求");
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        // 支持 Bearer token 格式
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 一次解析即拿到用户 ID（含过期校验），避免重复签名校验
        Long userId = StrUtil.isBlank(token) ? null : jwtUtil.getValidUserId(token);
        if (userId == null) {
            log.warn("无效或过期的 JWT - token 长度: {}, 验证失败", StrUtil.length(token));
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 将用户ID放入当前请求，由 CurrentUserContext 统一解析用户信息
        request.setAttribute(CurrentUserContext.USER_ID_ATTRIBUTE, userId);

        log.debug("JWT 验证成功 - 用户ID: {}", userId);
        return true;
    }

    /**
     * 写入 401 响应（复用统一 Result 结构）
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.unauthorized(message));
    }
}
