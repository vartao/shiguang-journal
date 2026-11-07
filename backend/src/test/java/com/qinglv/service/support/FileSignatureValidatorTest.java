package com.qinglv.service.support;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSignatureValidatorTest {

    /** 1x1 透明 PNG */
    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    @Test
    void acceptsRealPngContent() throws IOException {
        assertTrue(FileSignatureValidator.matches("png", new ByteArrayInputStream(TINY_PNG)));
    }

    @Test
    void rejectsTextContentNamedAsImage() throws IOException {
        byte[] fake = "this is definitely not an image".getBytes(StandardCharsets.UTF_8);
        assertFalse(FileSignatureValidator.matches("jpg", new ByteArrayInputStream(fake)));
        assertFalse(FileSignatureValidator.matches("png", new ByteArrayInputStream(fake)));
        assertFalse(FileSignatureValidator.matches("gif", new ByteArrayInputStream(fake)));
        assertFalse(FileSignatureValidator.matches("bmp", new ByteArrayInputStream(fake)));
    }

    @Test
    void rejectsValidMagicWithGarbageBody() throws IOException {
        // PNG 魔数 + 随机垃圾：魔数通过、解码失败
        byte[] forged = new byte[64];
        new java.util.Random(1).nextBytes(forged);
        System.arraycopy(TINY_PNG, 0, forged, 0, 8);
        assertFalse(FileSignatureValidator.matches("png", new ByteArrayInputStream(forged)));
    }

    @Test
    void acceptsWellFormedMagicForNonDecodableTypes() throws IOException {
        // webp: RIFF....WEBP
        byte[] webp = new byte[12];
        System.arraycopy("RIFF".getBytes(), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(), 0, webp, 8, 4);
        assertTrue(FileSignatureValidator.matches("webp", new ByteArrayInputStream(webp)));

        assertTrue(FileSignatureValidator.matches("pdf", new ByteArrayInputStream("%PDF-1.4 ...".getBytes())));
        assertTrue(FileSignatureValidator.matches("mp4", new ByteArrayInputStream(new byte[]{0,0,0,0,'f','t','y','p'})));
        assertTrue(FileSignatureValidator.matches("mp3", new ByteArrayInputStream("ID3".getBytes())));
        // 伪 jpg 内容声明为 txt：txt 无魔数要求，放行
        assertTrue(FileSignatureValidator.matches("txt", new ByteArrayInputStream(JPEG_MAGIC)));
    }

    @Test
    void acceptsRealJpegContent() throws Exception {
        var img = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", out);
        assertTrue(FileSignatureValidator.matches("jpg", new ByteArrayInputStream(out.toByteArray())));
        assertTrue(FileSignatureValidator.matches("jpeg", new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void rejectsMismatchedMagic() throws IOException {
        assertFalse(FileSignatureValidator.matches("webp", new ByteArrayInputStream("NOTRIFF".getBytes())));
        assertFalse(FileSignatureValidator.matches("pdf", new ByteArrayInputStream("GIF89a".getBytes())));
        assertFalse(FileSignatureValidator.matches("jpg", new ByteArrayInputStream(TINY_PNG)));
    }

    @Test
    void rejectsEmptyAndNullInput() throws IOException {
        assertFalse(FileSignatureValidator.matches("png", new ByteArrayInputStream(new byte[0])));
        assertFalse(FileSignatureValidator.matches(null, new ByteArrayInputStream(TINY_PNG)));
        assertFalse(FileSignatureValidator.matches("png", null));
    }
}
