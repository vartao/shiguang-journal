package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 导出控制器
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

    private final PdfExportService pdfExportService;
    private final CurrentUserContext currentUser;

    /**
     * 导出恋爱手账 PDF
     * GET /api/export/pdf?range=all
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false, defaultValue = "all") String range) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();

        byte[] pdf = pdfExportService.exportJournal(userId, coupleId, range);

        String filename = "shiguang-journal-" + range + ".pdf";
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
