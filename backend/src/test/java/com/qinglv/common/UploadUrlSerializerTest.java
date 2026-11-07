package com.qinglv.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadUrlSerializerTest {

    private final UploadUrlSigner signer = new UploadUrlSigner(
            "unit-test-sign-key-0123456789abcdef", 30, "/uploads/");

    private ObjectMapper mapperWithSigner() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new SimpleModule()
                .addSerializer(String.class, new UploadUrlSerializer(signer)));
        return mapper;
    }

    @Test
    void signsUploadPathInVoOutput() throws Exception {
        String json = mapperWithSigner().writeValueAsString(
                Map.of("avatar", "/uploads/2026/11/07/abc.jpg", "title", "标题"));

        assertTrue(json.contains("\"title\":\"标题\""));
        assertTrue(json.contains("avatar\":\"/uploads/2026/11/07/abc.jpg?exp="));
        assertTrue(json.contains("sig="));
    }

    @Test
    void signsEveryElementOfStringCollections() throws Exception {
        String json = mapperWithSigner().writeValueAsString(
                Map.of("photos", List.of("/uploads/a.jpg", "/uploads/b.jpg")));

        assertTrue(json.contains("/uploads/a.jpg?exp="));
        assertTrue(json.contains("/uploads/b.jpg?exp="));
    }

    @Test
    void leavesExternalUrlsAndPlainTextUntouched() throws Exception {
        assertEquals("\"https://cdn.example.com/qinglv/a.jpg\"",
                mapperWithSigner().writeValueAsString("https://cdn.example.com/qinglv/a.jpg"));
        assertEquals("\"普通文本\"", mapperWithSigner().writeValueAsString("普通文本"));
    }
}
