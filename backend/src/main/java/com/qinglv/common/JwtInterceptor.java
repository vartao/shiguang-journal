package com.qinglv.common;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT拦截器 - 验证请求中的token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /**
     * 存放当前用户ID的ThreadLocal
     */
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // OPTIONS预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        // 支持 Bearer token 格式
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (StrUtil.isBlank(token) || !jwtUtil.validateToken(token)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 将用户ID存入ThreadLocal
        Long userId = jwtUtil.getUserIdFromToken(token);
        CURRENT_USER.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清除ThreadLocal，防止内存泄漏
        CURRENT_USER.remove();
    }

    /**
     * 获取当前登录用户ID
     */
    public static Long getCurrentUserId() {
        return CURRENT_USER.get();
    }

    /**
     * 写入401响应
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}");
    }
}
