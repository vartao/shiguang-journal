package com.qinglv.controller;

import com.qinglv.common.UploadUrlSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadsControllerTest {

    private static final byte[] TINY_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @TempDir Path tempDir;

    private UploadUrlSigner signer;
    private UploadsController controller;

    @BeforeEach
    void setUp() throws Exception {
        signer = new UploadUrlSigner("unit-test-sign-key-0123456789abcdef", 30, "/uploads/");
        controller = new UploadsController(signer);
        ReflectionTestUtils.setField(controller, "uploadPath", tempDir.toString());
        Path file = tempDir.resolve("2026/11/07").resolve("test.png");
        Files.createDirectories(file.getParent());
        Files.write(file, TINY_PNG);
    }

    @Test
    void servesFileForSignedUrlWithCacheHeaders() throws Exception {
        String signed = signer.sign("/uploads/2026/11/07/test.png");

        mvc().perform(get(signed))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(TINY_PNG))
                .andExpect(header().string("Cache-Control", "public, max-age=2592000, immutable"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void servesThumbVariantWithSameSignature() throws Exception {
        Files.write(tempDir.resolve("2026/11/07").resolve("test_thumb.png"), TINY_PNG);
        String signed = signer.sign("/uploads/2026/11/07/test.png");

        mvc().perform(get("/uploads/2026/11/07/test_thumb.png")
                        .queryParam("exp", queryParam(signed, "exp"))
                        .queryParam("sig", queryParam(signed, "sig")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUnsignedRequest() throws Exception {
        mvc().perform(get("/uploads/2026/11/07/test.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsExpiredSignature() throws Exception {
        UploadUrlSigner expiredSigner = new UploadUrlSigner(
                "unit-test-sign-key-0123456789abcdef", -30, "/uploads/");
        UploadsController expiredController = new UploadsController(expiredSigner);
        ReflectionTestUtils.setField(expiredController, "uploadPath", tempDir.toString());

        String signed = expiredSigner.sign("/uploads/2026/11/07/test.png");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(expiredController).build();
        mvc.perform(get(signed)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsTamperedSignature() throws Exception {
        String signed = signer.sign("/uploads/2026/11/07/test.png");

        mvc().perform(get("/uploads/2026/11/07/test.png")
                        .queryParam("exp", queryParam(signed, "exp"))
                        .queryParam("sig", "deadbeef"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsPathTraversal() throws Exception {
        String signed = signer.sign("/uploads/../secret.png");

        mvc().perform(get(signed)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsMissingFile() throws Exception {
        String signed = signer.sign("/uploads/2026/11/07/absent.png");

        mvc().perform(get(signed)).andExpect(status().isNotFound());
    }

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(controller).build();
    }

    private String queryParam(String url, String name) {
        int queryIndex = url.indexOf('?');
        for (String pair : url.substring(queryIndex + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return "";
    }
}
