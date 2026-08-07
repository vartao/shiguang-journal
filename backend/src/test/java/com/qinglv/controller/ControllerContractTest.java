package com.qinglv.controller;

import com.qinglv.common.GlobalExceptionHandler;
import com.qinglv.dto.LoginVO;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.AnniversaryService;
import com.qinglv.service.AuthService;
import com.qinglv.service.FileService;
import com.qinglv.service.WishService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ControllerContractTest {

    private MockMvc mvc(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void authLoginRejectsBlankCredentialsWithStandardEnvelope() throws Exception {
        AuthService service = mock(AuthService.class);
        mvc(new AuthController(service)).perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void malformedJsonIsReportedAsBadRequestInsteadOfServerError() throws Exception {
        AuthService service = mock(AuthService.class);
        mvc(new AuthController(service)).perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"qing\","))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求格式不正确"));
    }

    @Test
    void authLoginReturnsStandardSuccessEnvelope() throws Exception {
        AuthService service = mock(AuthService.class);
        LoginVO login = new LoginVO();
        login.setToken("token");
        when(service.login(any())).thenReturn(login);
        mvc(new AuthController(service)).perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"qing\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("token"));
    }

    @Test
    void anniversaryRejectsMissingDate() throws Exception {
        AnniversaryService service = mock(AnniversaryService.class);
        UserMapper mapper = mock(UserMapper.class);
        mvc(new AnniversaryController(service, mapper)).perform(post("/api/anniversaries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"相识纪念日\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void wishCreateReturnsStandardEnvelope() throws Exception {
        WishService service = mock(WishService.class);
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setCoupleId(9L);
        when(mapper.selectById(any())).thenReturn(user);
        when(service.createWish(any(), anyLong(), any())).thenReturn(null);
        mvc(new WishController(service, mapper)).perform(post("/api/wishes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"一起看海\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("许愿成功"));
    }

    @Test
    void uploadRequiresMultipartFileAndWrapsSuccess() throws Exception {
        FileService service = mock(FileService.class);
        when(service.uploadFile(any(), any())).thenReturn("/uploads/demo.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "demo.jpg", "image/jpeg", new byte[]{1});
        mvc(new FileController(service)).perform(multipart("/api/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("/uploads/demo.jpg"));
    }
}
