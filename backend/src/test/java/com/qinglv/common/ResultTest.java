package com.qinglv.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一响应结构基础测试
 */
class ResultTest {

    @Test
    void success_shouldReturnCode200() {
        Result<String> result = Result.success("ok");
        assertEquals(200, result.getCode());
        assertEquals("ok", result.getData());
    }

    @Test
    void success_withMessage_shouldKeepData() {
        Result<Integer> result = Result.success("创建成功", 42);
        assertEquals(200, result.getCode());
        assertEquals("创建成功", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    void error_shouldUseDefaultOrCustomCode() {
        Result<Void> defaultError = Result.error("失败");
        assertNotNull(defaultError.getCode());
        assertEquals("失败", defaultError.getMessage());
        assertNull(defaultError.getData());

        Result<Void> custom = Result.error(403, "无权访问");
        assertEquals(403, custom.getCode());
        assertEquals("无权访问", custom.getMessage());
    }
}
