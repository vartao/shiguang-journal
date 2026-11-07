package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.CoupleUpdateDTO;
import com.qinglv.dto.CoupleVO;
import com.qinglv.entity.User;
import com.qinglv.service.CoupleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 情侣空间控制器
 * <p>
 * 处理情侣关系的创建、绑定、更新等核心操作
 */
@Slf4j
@RestController
@RequestMapping("/api/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;
    private final CurrentUserContext currentUser;

    /**
     * 获取情侣空间信息
     * GET /api/couple/info
     */
    @GetMapping("/info")
    public Result<CoupleVO> getCoupleInfo() {
        User user = currentUser.requireUser();
        log.debug("获取情侣信息 - userId: {}", user.getId());
        CoupleVO coupleVO = coupleService.getCoupleInfo(user);
        return Result.success(coupleVO);
    }

    /**
     * 创建情侣空间
     * POST /api/couple/create
     */
    @PostMapping("/create")
    public Result<CoupleVO> createCouple(@RequestBody(required = false) Map<String, String> body) {
        User user = currentUser.requireUser();
        log.info("用户创建情侣空间 - userId: {}", user.getId());
        CoupleVO coupleVO = coupleService.createCouple(user, body);
        return Result.success("创建成功", coupleVO);
    }

    /**
     * 绑定伴侣（邀请码）
     * POST /api/couple/bind
     */
    @PostMapping("/bind")
    public Result<CoupleVO> bindPartner(@RequestBody Map<String, String> body) {
        User user = currentUser.requireUser();
        String inviteCode = body != null ? body.get("inviteCode") : null;
        log.debug("用户尝试绑定伴侣 - userId: {}, inviteCode: {}", user.getId(), inviteCode);
        CoupleVO coupleVO = coupleService.bindPartner(user, inviteCode);
        return Result.success("绑定成功", coupleVO);
    }

    /**
     * 更新情侣空间信息
     * PUT /api/couple/update
     */
    @PutMapping("/update")
    public Result<CoupleVO> updateCoupleInfo(@Valid @RequestBody CoupleUpdateDTO dto) {
        User user = currentUser.requireUser();
        log.info("用户更新情侣信息 - userId: {}", user.getId());
        CoupleVO coupleVO = coupleService.updateCoupleInfo(user, dto);
        return Result.success("更新成功", coupleVO);
    }
}
