package com.qinglv.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 出参序列化时为 /uploads/ 相对路径追加短期签名。
 * 注册为全局 String 序列化器后，所有 VO 中携带的上传地址
 * （笔记图片、相册、足迹、头像、上传响应等）统一获得签名，
 * 数据库中的干净路径不受影响。
 */
public class UploadUrlSerializer extends JsonSerializer<String> {

    private final UploadUrlSigner signer;

    public UploadUrlSerializer(UploadUrlSigner signer) {
        this.signer = signer;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeString(signer.sign(value));
    }
}
