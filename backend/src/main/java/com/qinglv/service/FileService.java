package com.qinglv.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.qinglv.common.BusinessException;
import com.qinglv.entity.QlFile;
import com.qinglv.mapper.QlFileMapper;
import com.qinglv.service.support.FileSignatureValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final QlFileMapper fileMapper;
    private final OssStorageService ossStorageService;
    @Value("${file.upload-path}") private String uploadPath;
    @Value("${file.access-prefix}") private String accessPrefix;
    @Value("${file.allowed-types}") private String allowedTypes;
    @Value("${file.max-size}") private Long maxSize;
    @Value("${file.thumbnail-size:800}") private int thumbnailSize = 800;

    public String uploadFile(MultipartFile file, Long userId) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename();
        String extension = FileUtil.extName(originalFilename).toLowerCase();
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedName = IdUtil.fastSimpleUUID() + "." + extension;
        String relativePath = datePath + "/" + storedName;

        boolean useOss = ossStorageService.isEnabled();
        String fileUrl = useOss
                ? uploadToOss(file, extension, relativePath)
                : uploadToLocal(file, extension, relativePath);

        QlFile record = new QlFile();
        record.setUserId(userId); record.setOriginalName(originalFilename); record.setStoredName(storedName);
        record.setFilePath(relativePath); record.setFileUrl(fileUrl); record.setFileType(extension);
        record.setFileSize(file.getSize()); record.setContentType(file.getContentType());
        try { fileMapper.insert(record); }
        catch (RuntimeException e) {
            cleanupAfterFailure(useOss, relativePath);
            throw e;
        }
        log.info("文件上传成功: {} -> {}", originalFilename, fileUrl);
        return fileUrl;
    }

    private String uploadToLocal(MultipartFile file, String extension, String relativePath) {
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path destination = root.resolve(relativePath).normalize();
        if (!destination.startsWith(root)) throw new BusinessException("非法的上传路径");
        try {
            Files.createDirectories(destination.getParent());
            file.transferTo(destination.toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException("文件保存失败");
        }
        generateThumbnail(destination, extension);
        String prefix = accessPrefix.endsWith("/") ? accessPrefix : accessPrefix + "/";
        return prefix + relativePath;
    }

    private String uploadToOss(MultipartFile file, String extension, String relativePath) {
        try {
            byte[] bytes = file.getBytes();
            String fileUrl = ossStorageService.put(
                    ossStorageService.objectKey(relativePath),
                    new ByteArrayInputStream(bytes),
                    bytes.length,
                    file.getContentType());
            byte[] thumbnail = buildThumbnailBytes(bytes, extension);
            if (thumbnail != null) {
                ossStorageService.put(
                        ossStorageService.objectKey(thumbnailRelativePath(relativePath)),
                        new ByteArrayInputStream(thumbnail),
                        thumbnail.length,
                        "image/jpeg");
            }
            return fileUrl;
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new BusinessException("文件读取失败");
        }
    }

    private void cleanupAfterFailure(boolean useOss, String relativePath) {
        if (useOss) {
            ossStorageService.deleteQuietly(ossStorageService.objectKey(relativePath));
            ossStorageService.deleteQuietly(ossStorageService.objectKey(thumbnailRelativePath(relativePath)));
            return;
        }
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path destination = root.resolve(relativePath).normalize();
        try {
            Files.deleteIfExists(destination);
            Files.deleteIfExists(thumbnailPath(destination));
        } catch (IOException cleanupError) {
            log.error("上传记录失败后的文件清理也失败: {}", destination, cleanupError);
        }
    }

    private String thumbnailRelativePath(String relativePath) {
        int dotIndex = relativePath.lastIndexOf('.');
        String base = dotIndex > 0 ? relativePath.substring(0, dotIndex) : relativePath;
        return base + "_thumb.jpg";
    }

    private byte[] buildThumbnailBytes(byte[] sourceBytes, String extension) {
        if (!List.of("jpg", "jpeg", "png", "webp", "bmp").contains(extension)) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(new ByteArrayInputStream(sourceBytes))
                    .size(thumbnailSize, thumbnailSize)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.warn("生成缩略图失败", e);
            return null;
        }
    }

    public List<String> uploadFiles(MultipartFile[] files, Long userId) {
        if (files == null || files.length == 0) throw new BusinessException("上传文件不能为空");
        return Arrays.stream(files).map(file -> uploadFile(file, userId)).toList();
    }

    /**
     * 为静态图片生成 JPEG 缩略图，前端列表页按约定路径加载。
     * 生成失败不影响原图上传。
     */
    private void generateThumbnail(Path source, String extension) {
        if (!List.of("jpg", "jpeg", "png", "webp", "bmp").contains(extension)) {
            return;
        }
        try {
            Thumbnails.of(source.toFile())
                    .size(thumbnailSize, thumbnailSize)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toFile(thumbnailPath(source).toFile());
        } catch (IOException | RuntimeException e) {
            log.warn("生成缩略图失败: {}", source, e);
        }
    }

    private Path thumbnailPath(Path source) {
        String fileName = source.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
        return source.resolveSibling(baseName + "_thumb.jpg");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("上传文件不能为空");
        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) throw new BusinessException("文件名不能为空");
        if (file.getSize() > maxSize) throw new BusinessException("文件大小超出限制（最大" + (maxSize / 1024 / 1024) + "MB）");
        String extension = FileUtil.extName(originalFilename);
        if (StrUtil.isBlank(extension)) throw new BusinessException("无法识别文件类型");
        String ext = extension.toLowerCase();
        List<String> allowedList = Arrays.stream(allowedTypes.split(",")).map(String::trim).map(String::toLowerCase).toList();
        if (!allowedList.contains(ext)) throw new BusinessException("不支持的文件类型: " + extension);
        String contentType = file.getContentType();
        if (StrUtil.isBlank(contentType) || !isAllowedContentType(ext, contentType)) throw new BusinessException("文件内容类型与扩展名不匹配，或类型不被允许");
        try (InputStream in = file.getInputStream()) {
            if (!FileSignatureValidator.matches(ext, in)) throw new BusinessException("文件内容与扩展名不匹配");
        } catch (IOException e) {
            log.error("文件读取失败", e);
            throw new BusinessException("文件读取失败");
        }
    }

    private boolean isAllowedContentType(String ext, String contentType) {
        String ct = contentType.toLowerCase().split(";")[0].trim();
        return switch (ext) {
            case "jpg", "jpeg" -> ct.equals("image/jpeg") || ct.equals("image/jpg") || ct.equals("image/pjpeg");
            case "png" -> ct.equals("image/png") || ct.equals("image/x-png");
            case "gif" -> ct.equals("image/gif");
            case "webp" -> ct.equals("image/webp");
            case "bmp" -> ct.equals("image/bmp") || ct.equals("image/x-ms-bmp");
            case "mp4" -> ct.equals("video/mp4") || ct.equals("application/mp4");
            case "mp3" -> ct.equals("audio/mpeg") || ct.equals("audio/mp3");
            case "pdf" -> ct.equals("application/pdf");
            case "txt" -> ct.equals("text/plain");
            default -> false;
        };
    }
}
