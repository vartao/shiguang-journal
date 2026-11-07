package com.qinglv.common;

import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置 - 拦截器、CORS、上传 URL 出参签名
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RateLimiter rateLimiter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * 注册JWT拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/error",
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html"
                );

        // 频率限制拦截器 - 仅针对认证接口
        registry.addInterceptor(rateLimiter)
                .addPathPatterns("/api/auth/login", "/api/auth/register");
    }

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 出参统一签名：/uploads/ 相对路径追加 exp/sig，
     * 由 UploadsController 校验后返回文件（签名无效一律 404）
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer uploadUrlSignCustomizer(UploadUrlSigner uploadUrlSigner) {
        return builder -> builder.modulesToInstall(new SimpleModule("uploadUrlSign")
                .addSerializer(String.class, new UploadUrlSerializer(uploadUrlSigner)));
    }
}
