package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件上传控制器
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final CurrentUserContext currentUser;

    /**
     * 单文件上传
     * POST /api/upload
     */
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        Long userId = currentUser.getUserId();
        String fileUrl = fileService.uploadFile(file, userId);
        return Result.success("上传成功", fileUrl);
    }

    /**
     * 多文件上传
     * POST /api/upload/batch
     */
    @PostMapping("/batch")
    public Result<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        Long userId = currentUser.getUserId();
        List<String> urls = fileService.uploadFiles(files, userId);
        return Result.success("上传成功", urls);
    }
}
