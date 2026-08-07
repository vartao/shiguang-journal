package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.CoupleUpdateDTO;
import com.qinglv.dto.CoupleVO;
import com.qinglv.service.CoupleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 情侣空间控制器
 */
@RestController
@RequestMapping("/api/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    /**
     * 获取情侣空间信息
     * GET /api/couple/info
     */
    @GetMapping("/info")
    public Result<CoupleVO> getCoupleInfo() {
        Long userId = JwtInterceptor.getCurrentUserId();
        CoupleVO coupleVO = coupleService.getCoupleInfo(userId);
        return Result.success(coupleVO);
    }

    /**
     * 创建情侣空间
     * POST /api/couple/create
     */
    @PostMapping("/create")
    public Result<CoupleVO> createCouple(@RequestBody(required = false) Map<String, String> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        CoupleVO coupleVO = coupleService.createCouple(userId, body);
        return Result.success("创建成功", coupleVO);
    }

    /**
     * 绑定伴侣（邀请码）
     * POST /api/couple/bind
     */
    @PostMapping("/bind")
    public Result<CoupleVO> bindPartner(@RequestBody Map<String, String> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        String inviteCode = body != null ? body.get("inviteCode") : null;
        CoupleVO coupleVO = coupleService.bindPartner(userId, inviteCode);
        return Result.success("绑定成功", coupleVO);
    }

    /**
     * 更新情侣空间信息
     * PUT /api/couple/update
     */
    @PutMapping("/update")
    public Result<CoupleVO> updateCoupleInfo(@Valid @RequestBody CoupleUpdateDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        CoupleVO coupleVO = coupleService.updateCoupleInfo(userId, dto);
        return Result.success("更新成功", coupleVO);
    }
}
