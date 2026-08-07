package com.qinglv.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置。accessKeyId / accessKeySecret 为空时自动回退到本地磁盘存储。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.oss")
public class OssProperties {

    /**
     * OSS endpoint，例如 oss-cn-wuhan-lr.aliyuncs.com
     */
    private String endpoint;

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * AccessKey ID（建议只授予单个 bucket 的最小权限）
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * 公开访问基础 URL，例如 https://bucket.oss-cn-wuhan-lr.aliyuncs.com 或 CDN 域名
     */
    private String publicBaseUrl;

    /**
     * 对象 key 前缀，用于在同一 bucket 内隔离不同项目
     */
    private String keyPrefix = "qinglv/";
}
