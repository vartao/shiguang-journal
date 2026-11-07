package com.qinglv.service.support;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.List;

/**
 * 上传文件魔数与图片解码校验。
 * <p>
 * 扩展名和 Content-Type 都由客户端声明、可以伪造，这里以文件头字节为准：
 * 先按扩展名比对魔数，jpg/png/gif/bmp 再做一次解码验证，
 * 拦截"合法文件头 + 垃圾内容"的伪造文件。
 * <p>
 * 注意：不做解码重编码——Java 图像编码不处理 EXIF 方向，
 * 重编码会让手机竖拍照片横倒；剥除元数据需配合方向旋转，暂不引入。
 */
public final class FileSignatureValidator {

    private static final int HEAD_LENGTH = 32;
    private static final List<String> DECODABLE_TYPES = List.of("jpg", "jpeg", "png", "gif", "bmp");

    private FileSignatureValidator() {}

    /**
     * 校验文件头是否与扩展名匹配。txt 无固定魔数跳过；
     * 未定义规则的扩展名放行（白名单校验已在前置步骤完成）。
     *
     * @param extension 小写扩展名
     * @param in        文件输入流（校验后流上数据不再可用，调用方需重新获取）
     */
    public static boolean matches(String extension, InputStream in) throws IOException {
        if (extension == null || in == null) {
            return false;
        }
        byte[] head = in.readNBytes(HEAD_LENGTH);
        if (head.length == 0) {
            return false;
        }
        boolean magicOk = switch (extension) {
            case "jpg", "jpeg" -> startsWith(head, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
            case "png" -> startsWith(head, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "gif" -> startsWith(head, "GIF8".getBytes());
            case "webp" -> head.length >= 12 && startsWith(head, "RIFF".getBytes())
                    && Arrays.equals(Arrays.copyOfRange(head, 8, 12), "WEBP".getBytes());
            case "bmp" -> startsWith(head, "BM".getBytes());
            case "pdf" -> startsWith(head, "%PDF-".getBytes());
            case "mp4" -> head.length >= 8 && Arrays.equals(Arrays.copyOfRange(head, 4, 8), "ftyp".getBytes());
            case "mp3" -> startsWith(head, "ID3".getBytes())
                    || (head.length >= 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xE0) == 0xE0);
            case "txt" -> true;
            default -> true;
        };
        if (!magicOk) {
            return false;
        }
        if (DECODABLE_TYPES.contains(extension)) {
            // 把已读的文件头拼回流前面，交给 ImageIO 完整解码验证；
            // 损坏流可能抛异常而非返回 null，一律视为校验失败
            try {
                return ImageIO.read(new SequenceInputStream(
                        new java.io.ByteArrayInputStream(head), in)) != null;
            } catch (IOException | RuntimeException e) {
                return false;
            }
        }
        return true;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        return data.length >= prefix.length
                && Arrays.equals(Arrays.copyOfRange(data, 0, prefix.length), prefix);
    }
}
