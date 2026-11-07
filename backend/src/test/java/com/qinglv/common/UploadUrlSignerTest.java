package com.qinglv.common;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadUrlSignerTest {

    private final UploadUrlSigner signer = new UploadUrlSigner(
            "unit-test-sign-key-0123456789abcdef", 30, "/uploads/");

    @Test
    void signsUploadPathWithExpiryAndSignature() {
        String signed = signer.sign("/uploads/2026/11/07/abc.jpg");

        assertTrue(signed.startsWith("/uploads/2026/11/07/abc.jpg?exp="));
        String exp = queryParam(signed, "exp");
        String sig = queryParam(signed, "sig");
        assertFalse(sig.isBlank());

        // 30 天有效期
        long expectedExp = Instant.now().getEpochSecond() + 30L * 24 * 60 * 60;
        assertTrue(Math.abs(Long.parseLong(exp) - expectedExp) < 5);

        // 相对路径（不含前缀）与原图路径都校验通过
        assertTrue(signer.isValid("2026/11/07/abc.jpg", exp, sig));
    }

    @Test
    void thumbVariantSharesSameSignature() {
        String signed = signer.sign("/uploads/2026/11/07/abc.jpg");
        String exp = queryParam(signed, "exp");
        String sig = queryParam(signed, "sig");

        assertTrue(signer.isValid("2026/11/07/abc_thumb.jpg", exp, sig));
        assertTrue(signer.isValid("2026/11/07/abc_thumb.jpg", exp, sig));
    }

    @Test
    void leavesNonUploadStringsUnchanged() {
        assertEquals("https://cdn.example.com/qinglv/2026/a.jpg", signer.sign("https://cdn.example.com/qinglv/2026/a.jpg"));
        assertEquals("", signer.sign(""));
        assertNull(signer.sign(null));
        assertEquals("普通文本", signer.sign("普通文本"));
    }

    @Test
    void replacesExistingQueryWhenSigning() {
        String signed = signer.sign("/uploads/2026/11/07/abc.jpg?exp=1&sig=stale");
        assertFalse(signed.contains("stale"));
        String exp = queryParam(signed, "exp");
        String sig = queryParam(signed, "sig");
        assertTrue(signer.isValid("2026/11/07/abc.jpg", exp, sig));
    }

    @Test
    void rejectsTamperedSignatureAndWrongPath() {
        String signed = signer.sign("/uploads/2026/11/07/abc.jpg");
        String exp = queryParam(signed, "exp");
        String sig = queryParam(signed, "sig");

        assertFalse(signer.isValid("2026/11/07/other.jpg", exp, sig));
        assertFalse(signer.isValid("2026/11/07/abc.jpg", exp, sig + "0"));
        assertFalse(signer.isValid("2026/11/07/abc.jpg", exp, null));
        assertFalse(signer.isValid("2026/11/07/abc.jpg", "not-a-number", sig));
    }

    @Test
    void rejectsExpiredSignature() {
        String signed = signer.sign("/uploads/2026/11/07/abc.jpg");
        String sig = queryParam(signed, "sig");
        String expired = String.valueOf(Instant.now().getEpochSecond() - 1);

        assertFalse(signer.isValid("2026/11/07/abc.jpg", expired, sig));
    }

    @Test
    void differentKeyProducesDifferentSignature() {
        UploadUrlSigner other = new UploadUrlSigner(
                "another-sign-key-0123456789abcdef00", 30, "/uploads/");
        assertNotEquals(other.sign("/uploads/2026/11/07/abc.jpg"), signer.sign("/uploads/2026/11/07/abc.jpg"));
    }

    @Test
    void toStoredPathStripsQueryOnly() {
        assertEquals("/uploads/a.jpg", UploadUrls.toStoredPath("/uploads/a.jpg?exp=1&sig=ab"));
        assertEquals("/uploads/a.jpg", UploadUrls.toStoredPath("/uploads/a.jpg"));
        assertNull(UploadUrls.toStoredPath(null));
    }

    @Test
    void canonicalKeyIgnoresPrefixExtensionAndThumb() {
        assertEquals("2026/11/07/abc", UploadUrls.canonicalKey("/uploads/2026/11/07/abc.jpg", "/uploads/"));
        assertEquals("2026/11/07/abc", UploadUrls.canonicalKey("/uploads/2026/11/07/abc_thumb.jpg", "/uploads/"));
        assertEquals("2026/11/07/abc", UploadUrls.canonicalKey("2026/11/07/abc.jpg", "/uploads/"));
        assertEquals("", UploadUrls.canonicalKey(null, "/uploads/"));
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
