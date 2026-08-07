package com.qinglv.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.qinglv.common.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;

/**
 * 阿里云 OSS 存储。仅在配置完整时启用，否则由调用方回退到本地磁盘。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssStorageService {

    private final OssProperties properties;

    public boolean isEnabled() {
        return StringUtils.hasText(properties.getEndpoint())
                && StringUtils.hasText(properties.getBucket())
                && StringUtils.hasText(properties.getAccessKeyId())
                && StringUtils.hasText(properties.getAccessKeySecret())
                && StringUtils.hasText(properties.getPublicBaseUrl());
    }

    /**
     * 上传对象并返回公开访问 URL。
     */
    public String put(String objectKey, InputStream inputStream, long sizeBytes, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(sizeBytes);
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }

        OSS client = client();
        try (inputStream) {
            client.putObject(properties.getBucket(), objectKey, inputStream, metadata);
            return publicUrl(objectKey);
        } catch (Exception e) {
            log.error("OSS 上传失败: {}", objectKey, e);
            throw new IllegalStateException("OSS 上传失败", e);
        } finally {
            client.shutdown();
        }
    }

    /**
     * 删除对象（失败只记录日志，不影响主流程）。
     */
    public void deleteQuietly(String objectKey) {
        OSS client = client();
        try {
            client.deleteObject(properties.getBucket(), objectKey);
        } catch (Exception e) {
            log.warn("OSS 删除失败: {}", objectKey, e);
        } finally {
            client.shutdown();
        }
    }

    public String publicUrl(String objectKey) {
        String base = properties.getPublicBaseUrl();
        return base.endsWith("/") ? base + objectKey : base + "/" + objectKey;
    }

    /**
     * 将相对路径转为带项目前缀的对象 key，便于多项目共用一个 bucket。
     */
    public String objectKey(String relativePath) {
        String prefix = properties.getKeyPrefix();
        if (!StringUtils.hasText(prefix)) {
            return relativePath;
        }
        return prefix.endsWith("/") ? prefix + relativePath : prefix + "/" + relativePath;
    }

    private OSS client() {
        String endpoint = properties.getEndpoint();
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            endpoint = "https://" + endpoint;
        }
        return new OSSClientBuilder().build(endpoint, properties.getAccessKeyId(), properties.getAccessKeySecret());
    }
}
