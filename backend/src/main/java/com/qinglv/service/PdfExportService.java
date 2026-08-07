package com.qinglv.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.NoteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * 手账 PDF 导出服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final NoteService noteService;

    @Value("${file.upload-path:uploads}")
    private String uploadPath;

    @Value("${file.access-prefix:/uploads/}")
    private String accessPrefix;

    private static final Color BROWN = new Color(92, 61, 46);
    private static final Color BROWN_LIGHT = new Color(139, 110, 90);
    private static final Color PAPER = new Color(252, 248, 240);

    /**
     * 生成恋爱手账 PDF
     *
     * @param userId   当前用户
     * @param coupleId 情侣空间 ID
     * @param range    时间范围：all / 30d / 90d / year
     */
    public byte[] exportJournal(Long userId, Long coupleId, String range) {
        List<NoteVO> notes = noteService.exportNotes(userId, coupleId, range);
        String rangeLabel = getRangeLabel(range);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 50, 50, 60, 60);
            PdfWriter.getInstance(document, out);
            document.open();

            addCover(document, rangeLabel, notes.size());
            for (NoteVO note : notes) {
                addNotePage(document, note);
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("生成 PDF 失败", e);
            throw new BusinessException("PDF 生成失败");
        }
    }

    private void addCover(Document document, String rangeLabel, int count) throws DocumentException, IOException {
        BaseFont baseFont = createBaseFont();
        Font titleFont = new Font(baseFont, 32, Font.BOLD, BROWN);
        Font metaFont = new Font(baseFont, 14, Font.NORMAL, BROWN_LIGHT);
        Font dateFont = new Font(baseFont, 12, Font.NORMAL, BROWN_LIGHT);

        Paragraph title = new Paragraph("拾光手账", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(24);
        document.add(title);

        Paragraph divider = new Paragraph("✦", new Font(baseFont, 18, Font.NORMAL, new Color(212, 165, 116)));
        divider.setAlignment(Element.ALIGN_CENTER);
        divider.setSpacingAfter(24);
        document.add(divider);

        Paragraph meta = new Paragraph("共 " + count + " 篇笔记 · " + rangeLabel, metaFont);
        meta.setAlignment(Element.ALIGN_CENTER);
        meta.setSpacingAfter(8);
        document.add(meta);

        Paragraph date = new Paragraph("导出于 " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日")), dateFont);
        date.setAlignment(Element.ALIGN_CENTER);
        document.add(date);

        document.newPage();
    }

    private void addNotePage(Document document, NoteVO note) throws DocumentException, IOException {
        BaseFont baseFont = createBaseFont();
        Font titleFont = new Font(baseFont, 20, Font.BOLD, BROWN);
        Font dateFont = new Font(baseFont, 11, Font.NORMAL, BROWN_LIGHT);
        Font tagFont = new Font(baseFont, 10, Font.NORMAL, new Color(120, 90, 70));
        Font contentFont = new Font(baseFont, 12, Font.NORMAL, new Color(61, 43, 31));
        Font authorFont = new Font(baseFont, 11, Font.ITALIC, BROWN_LIGHT);

        document.add(new Paragraph(note.getTitle(), titleFont));

        String dateText = note.getNoteDate() != null
                ? note.getNoteDate().format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日"))
                : "";
        Paragraph dateLine = new Paragraph(dateText, dateFont);
        dateLine.setSpacingAfter(6);
        document.add(dateLine);

        String mood = Objects.toString(note.getMood(), "");
        String weather = Objects.toString(note.getWeather(), "");
        if (!mood.isEmpty() || !weather.isEmpty()) {
            StringBuilder tagBuilder = new StringBuilder();
            if (!mood.isEmpty()) {
                tagBuilder.append(getMoodLabel(mood));
            }
            if (!weather.isEmpty()) {
                if (tagBuilder.length() > 0) tagBuilder.append("  ·  ");
                tagBuilder.append(getWeatherLabel(weather));
            }
            Paragraph tags = new Paragraph(tagBuilder.toString(), tagFont);
            tags.setSpacingAfter(12);
            document.add(tags);
        }

        if (note.getContent() != null && !note.getContent().isEmpty()) {
            Paragraph content = new Paragraph(note.getContent(), contentFont);
            content.setLeading(20);
            content.setSpacingAfter(12);
            document.add(content);
        }

        if (note.getImages() != null && !note.getImages().isEmpty()) {
            for (String imageUrl : note.getImages()) {
                addImage(document, imageUrl);
            }
        }

        Paragraph author = new Paragraph("—— " + (note.getAuthorName() != null ? note.getAuthorName() : "我们"), authorFont);
        author.setAlignment(Element.ALIGN_RIGHT);
        author.setSpacingBefore(12);
        document.add(author);

        document.newPage();
    }

    private void addImage(Document document, String imageUrl) {
        try {
            Path imagePath = resolveImagePath(imageUrl);
            if (imagePath == null || !imagePath.toFile().exists()) {
                return;
            }
            Image image = Image.getInstance(imagePath.toAbsolutePath().toString());
            float width = document.getPageSize().getWidth() - document.leftMargin() - document.rightMargin();
            image.scaleToFit(width * 0.85f, 240);
            image.setBorderWidth(0);
            image.setAlignment(Element.ALIGN_CENTER);
            document.add(image);
        } catch (Exception e) {
            log.warn("PDF 嵌入图片失败: {}", imageUrl, e);
        }
    }

    private Path resolveImagePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String prefix = accessPrefix.endsWith("/") ? accessPrefix : accessPrefix + "/";
        String relative = imageUrl;
        if (imageUrl.startsWith(prefix)) {
            relative = imageUrl.substring(prefix.length());
        } else if (imageUrl.startsWith("/")) {
            relative = imageUrl.substring(1);
        }
        Path root = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            return null;
        }
        return resolved;
    }

    private BaseFont createBaseFont() throws DocumentException, IOException {
        return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
    }

    private String getRangeLabel(String range) {
        return switch (range) {
            case "30d" -> "近 30 天";
            case "90d" -> "近 90 天";
            case "year" -> "今年";
            default -> "全部";
        };
    }

    private String getMoodLabel(String mood) {
        return switch (mood) {
            case "happy" -> "😊 开心";
            case "love" -> "🥰 甜蜜";
            case "excited" -> "🤩 兴奋";
            case "calm" -> "😌 平静";
            case "miss" -> "🥺 想念";
            case "funny" -> "🤣 搞笑";
            case "sad" -> "😢 难过";
            case "angry" -> "😤 生气";
            default -> "💕 记录";
        };
    }

    private String getWeatherLabel(String weather) {
        return switch (weather) {
            case "sunny" -> "☀️ 晴天";
            case "cloudy" -> "⛅ 多云";
            case "rainy" -> "🌧️ 下雨";
            case "snowy" -> "❄️ 下雪";
            case "windy" -> "🌬️ 刮风";
            default -> "";
        };
    }
}
