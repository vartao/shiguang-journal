package com.qinglv.common;

/**
 * 上传资源 URL 的纯字符串处理：剥离查询参数、计算参与签名的规范化 key。
 * <p>
 * 规范化 key 去掉访问前缀、扩展名与 {@code _thumb} 后缀，
 * 使原图与前端按约定改写的缩略图共用同一签名。
 */
public final class UploadUrls {

    private static final String THUMBNAIL_SUFFIX = "_thumb";

    private UploadUrls() {}

    /**
     * 剥离 URL 上的查询参数（?exp=...&sig=...），用于写路径入库前清洗，
     * 避免带过期时间的签名 URL 被持久化。
     */
    public static String toStoredPath(String url) {
        if (url == null) {
            return null;
        }
        int queryIndex = url.indexOf('?');
        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }

    /**
     * 计算参与签名的规范化 key：去访问前缀、去 query、去扩展名与 _thumb 后缀。
     * 例如 /uploads/2026/11/07/abc.jpg 与 /uploads/2026/11/07/abc_thumb.jpg 均得到 2026/11/07/abc。
     */
    public static String canonicalKey(String url, String accessPrefix) {
        String path = toStoredPath(url);
        if (path == null) {
            return "";
        }
        if (!accessPrefix.isEmpty() && path.startsWith(accessPrefix)) {
            path = path.substring(accessPrefix.length());
        }
        int slashIndex = path.lastIndexOf('/');
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex > slashIndex + 1) {
            path = path.substring(0, dotIndex);
        }
        if (path.endsWith(THUMBNAIL_SUFFIX)) {
            path = path.substring(0, path.length() - THUMBNAIL_SUFFIX.length());
        }
        return path;
    }
}
