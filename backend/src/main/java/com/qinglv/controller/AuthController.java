package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.LoginDTO;
import com.qinglv.dto.LoginVO;
import com.qinglv.dto.RegisterDTO;
import com.qinglv.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器
 * <p>
 * 处理用户登录、注册、头像更新、资料修改等核心认证相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "登录注册 / 用户资料")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserContext currentUser;

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO) {
        log.debug("用户尝试登录 - username: {}", loginDTO.getUsername());
        LoginVO loginVO = authService.login(loginDTO);
        log.info("用户登录成功 - username: {}", loginDTO.getUsername());
        return Result.success("登录成功", loginVO);
    }

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO registerDTO) {
        log.debug("新用户注册 - username: {}", registerDTO.getUsername());
        LoginVO loginVO = authService.register(registerDTO);
        log.info("用户注册成功 - username: {}", registerDTO.getUsername());
        return Result.success("注册成功", loginVO);
    }

    /**
     * 获取当前用户资料
     * GET /api/auth/profile
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户资料")
    public Result<LoginVO.UserInfo> profile() {
        Long userId = currentUser.getUserId();
        log.debug("获取用户资料 - userId: {}", userId);
        return Result.success(authService.getProfile(userId));
    }

    /**
     * 更新头像
     * PUT /api/auth/avatar
     */
    @PutMapping("/avatar")
    @Operation(summary = "更新头像")
    public Result<LoginVO.UserInfo> updateAvatar(@RequestBody Map<String, String> body) {
        Long userId = currentUser.getUserId();
        String avatar = body != null ? body.get("avatar") : null;
        log.info("用户更新头像 - userId: {}", userId);
        return Result.success("更新成功", authService.updateAvatar(userId, avatar));
    }

    /**
     * 更新个人资料（昵称 / 头像）
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public Result<LoginVO.UserInfo> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = currentUser.getUserId();
        String nickname = body != null ? body.get("nickname") : null;
        String avatar = body != null ? body.get("avatar") : null;
        log.info("用户更新资料 - userId: {}, nickname: {}", userId, nickname);
        return Result.success("更新成功", authService.updateProfile(userId, nickname, avatar));
    }
}
