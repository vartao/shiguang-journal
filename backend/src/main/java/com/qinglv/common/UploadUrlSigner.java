package com.qinglv.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * 上传资源签名器：为 /uploads/ 相对路径生成带过期时间的 HMAC 签名。
 * <p>
 * 签名只在接口出参边界添加（见 {@link UploadUrlSerializer}），
 * 数据库始终存干净路径；前端缩略图改写、小程序 URL 还原等
 * 不改变规范化 key 的操作不影响校验。签名密钥默认复用 JWT_SECRET，
 * 生产可用 FILE_SIGN_KEY 独立配置。
 */
@Slf4j
@Component
public class UploadUrlSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final char PAYLOAD_SEPARATOR = '.';

    private final byte[] signKey;
    private final long ttlSeconds;
    private final String accessPrefix;

    public UploadUrlSigner(
            @Value("${file.sign-key:${jwt.secret}}") String signKey,
            @Value("${file.url-ttl-days:30}") long urlTtlDays,
            @Value("${file.access-prefix:/uploads/}") String accessPrefix) {
        this.signKey = signKey.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = TimeUnit.DAYS.toSeconds(urlTtlDays);
        this.accessPrefix = accessPrefix;
        if (signKey.contains("dev-only")) {
            log.warn("===== WARNING: 上传签名使用默认密钥！生产环境请设置 FILE_SIGN_KEY =====");
        }
    }

    /**
     * 为访问前缀开头的相对路径追加 exp/sig 查询参数；
     * 其余字符串（OSS 公开外链、普通文本等）原样返回。
     */
    public String sign(String url) {
        if (url == null || url.isBlank() || !url.startsWith(accessPrefix)) {
            return url;
        }
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String cleanPath = UploadUrls.toStoredPath(url);
        return cleanPath + "?exp=" + exp + "&sig=" + signature(UploadUrls.canonicalKey(cleanPath, accessPrefix), exp);
    }

    /**
     * 校验请求路径上的签名与有效期，任何缺失、过期或篡改都返回 false。
     *
     * @param relativePath 不含查询参数的相对路径（如 2026/11/07/abc.jpg）
     * @param exp          签名过期时间（epoch 秒）
     * @param sig          签名值（hex）
     */
    public boolean isValid(String relativePath, String exp, String sig) {
        if (relativePath == null || relativePath.isBlank() || exp == null || sig == null) {
            return false;
        }
        long expValue;
        try {
            expValue = Long.parseLong(exp);
        } catch (NumberFormatException e) {
            return false;
        }
        if (expValue < Instant.now().getEpochSecond()) {
            return false;
        }
        String expected = signature(UploadUrls.canonicalKey(relativePath, accessPrefix), expValue);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                sig.getBytes(StandardCharsets.UTF_8));
    }

    private String signature(String canonicalKey, long exp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signKey, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((canonicalKey + PAYLOAD_SEPARATOR + exp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("上传 URL 签名计算失败", e);
        }
    }
}
