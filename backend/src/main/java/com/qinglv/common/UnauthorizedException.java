package com.qinglv.common;

/**
 * 认证异常 —— Token 缺失 / 失效，或对应用户已不存在。
 * <p>
 * 区别于 {@link BusinessException}：本异常映射为真实 HTTP 401，
 * 便于前端按状态码触发重新登录。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
