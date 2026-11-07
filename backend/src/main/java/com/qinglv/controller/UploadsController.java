package com.qinglv.controller;

import com.qinglv.common.UploadUrlSigner;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 上传文件访问控制器：校验签名 URL 后流式返回文件。
 * <p>
 * 签名无效、过期或路径越界的请求一律 404，不泄露失败原因；
 * 原图与 _thumb 缩略图共用同一签名（规范化 key 不含扩展名与 _thumb 后缀）。
 */
@RestController
@RequiredArgsConstructor
public class UploadsController {

    private static final String ACCESS_PREFIX = "/uploads/";
    private static final long CACHE_MAX_AGE_SECONDS = 30L * 24 * 60 * 60;

    private final UploadUrlSigner uploadUrlSigner;

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * GET /uploads/{yyyy/MM/dd/文件名}
     */
    @GetMapping(ACCESS_PREFIX + "**")
    public ResponseEntity<Resource> serve(HttpServletRequest request) {
        String relativePath = extractRelativePath(request.getRequestURI());
        String exp = request.getParameter("exp");
        String sig = request.getParameter("sig");
        if (relativePath == null || !uploadUrlSigner.isValid(relativePath, exp, sig)) {
            return ResponseEntity.notFound().build();
        }

        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE_SECONDS + ", immutable")
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaTypeFactory.getMediaType(relativePath).orElse(MediaType.APPLICATION_OCTET_STREAM))
                .body(new FileSystemResource(file));
    }

    private String extractRelativePath(String requestUri) {
        if (requestUri == null || !requestUri.startsWith(ACCESS_PREFIX)) {
            return null;
        }
        try {
            String relative = URLDecoder.decode(requestUri.substring(ACCESS_PREFIX.length()), StandardCharsets.UTF_8);
            return relative.isBlank() ? null : relative;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
