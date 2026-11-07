package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.mapper.QlFileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FileServiceTest {
    /** 1x1 透明 PNG，通过魔数与解码校验的最小合法图片 */
    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @TempDir Path tempDir;
    QlFileMapper mapper;
    OssStorageService ossStorageService;
    FileService service;
    @BeforeEach void setUp() {
        mapper = mock(QlFileMapper.class);
        ossStorageService = mock(OssStorageService.class);
        service = new FileService(mapper, ossStorageService);
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        ReflectionTestUtils.setField(service, "accessPrefix", "/uploads/");
        ReflectionTestUtils.setField(service, "allowedTypes", "jpg,jpeg,png");
        ReflectionTestUtils.setField(service, "maxSize", 1024L);
    }
    @Test void rejectsBlankFilenameAndMismatchedMime() {
        assertThrows(BusinessException.class, () -> service.uploadFile(new MockMultipartFile("file", "", "image/jpeg", new byte[]{1}), 1L));
        assertThrows(BusinessException.class, () -> service.uploadFile(new MockMultipartFile("file", "x.jpg", "text/plain", new byte[]{1}), 1L));
    }
    @Test void databaseFailureRemovesWrittenFile() throws Exception {
        when(mapper.insert(any())).thenThrow(new RuntimeException("db down"));
        assertThrows(RuntimeException.class, () -> service.uploadFile(new MockMultipartFile("file", "x.png", "image/png", TINY_PNG), 1L));
        try (var files = Files.walk(tempDir)) { assertEquals(0, files.filter(Files::isRegularFile).count()); }
    }
    @Test void writesInsideNormalizedRootAndReturnsUrl() throws Exception {
        when(mapper.insert(any())).thenReturn(1);
        String url = service.uploadFile(new MockMultipartFile("file", "x.PNG", "image/png", TINY_PNG), 1L);
        assertTrue(url.startsWith("/uploads/"));
        // 合法 PNG 会同时写出原图与 _thumb.jpg 缩略图
        try (var files = Files.walk(tempDir)) { assertEquals(2, files.filter(Files::isRegularFile).count()); }
    }

    @Test void rejectsForgedImageContent() {
        // 扩展名与 MIME 都合法，但文件头不是 JPEG（伪造内容必须被拒绝）
        assertThrows(BusinessException.class, () -> service.uploadFile(
                new MockMultipartFile("file", "x.jpg", "image/jpeg", "hello fake jpg".getBytes()), 1L));
    }

    @Test void generatesJpegThumbnailForValidImage() throws Exception {
        when(mapper.insert(any())).thenReturn(1);
        // 1x1 透明 PNG
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        String url = service.uploadFile(new MockMultipartFile("file", "x.png", "image/png", png), 1L);
        assertTrue(url.startsWith("/uploads/"));
        try (var files = Files.walk(tempDir)) {
            List<Path> written = files.filter(Files::isRegularFile).toList();
            assertEquals(2, written.size());
            assertTrue(written.stream().anyMatch(p -> p.getFileName().toString().endsWith("_thumb.jpg")));
        }
    }

    @Test void uploadsToOssWhenEnabled() throws Exception {
        when(ossStorageService.isEnabled()).thenReturn(true);
        when(ossStorageService.objectKey(anyString())).thenAnswer(inv -> "qinglv/" + inv.getArgument(0));
        when(ossStorageService.put(anyString(), any(), anyLong(), any()))
                .thenAnswer(inv -> "https://cdn.example.com/" + inv.getArgument(0));
        when(mapper.insert(any())).thenReturn(1);
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
        String url = service.uploadFile(new MockMultipartFile("file", "x.png", "image/png", png), 1L);
        assertTrue(url.startsWith("https://cdn.example.com/qinglv/"));
        // 原图 + 缩略图都走 OSS，不落本地磁盘
        verify(ossStorageService, times(2)).put(anyString(), any(), anyLong(), any());
        try (var files = Files.walk(tempDir)) { assertEquals(0, files.filter(Files::isRegularFile).count()); }
    }
}
