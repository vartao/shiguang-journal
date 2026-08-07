package com.qinglv.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简易频率限制器 - 基于IP的滑动窗口
 */
@Component
public class RateLimiter implements HandlerInterceptor {

    private static final int MAX_REQUESTS = 10; // 10 requests per window
    private static final long WINDOW_MS = 60_000; // 1 minute
    private static final long CLEANUP_INTERVAL_MS = WINDOW_MS;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong nextCleanupAt = new AtomicLong();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long now = System.currentTimeMillis();
        cleanupExpiredCounters(now);
        String ip = getClientIp(request);
        WindowCounter counter = counters.compute(ip, (key, existing) -> {
            if (existing == null || now - existing.windowStart > WINDOW_MS) {
                return new WindowCounter(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (counter.count.get() > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        // Nginx overwrites X-Real-IP/X-Forwarded-For, so prefer the single-hop value.
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Take first IP if multiple proxies
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private void cleanupExpiredCounters(long now) {
        long scheduled = nextCleanupAt.get();
        if (now < scheduled || !nextCleanupAt.compareAndSet(scheduled, now + CLEANUP_INTERVAL_MS)) {
            return;
        }
        counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS);
    }

    private record WindowCounter(long windowStart, AtomicInteger count) {}
}
