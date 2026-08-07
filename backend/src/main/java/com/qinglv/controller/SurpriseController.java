package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.SurpriseDTO;
import com.qinglv.dto.SurpriseVO;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.SurpriseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 惊喜彩蛋控制器
 */
@RestController
@RequestMapping("/api/surprises")
@RequiredArgsConstructor
public class SurpriseController {

    private final SurpriseService surpriseService;
    private final UserMapper userMapper;

    /**
     * 获取所有惊喜彩蛋
     * GET /api/surprises
     */
    @GetMapping
    public Result<List<SurpriseVO>> getSurprises() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        List<SurpriseVO> list = surpriseService.getSurprises(coupleId);
        return Result.success(list);
    }

    /**
     * 创建惊喜彩蛋
     * POST /api/surprises
     */
    @PostMapping
    public Result<SurpriseVO> createSurprise(@Valid @RequestBody SurpriseDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        SurpriseVO vo = surpriseService.createSurprise(userId, coupleId, dto);
        return Result.success("创建成功", vo);
    }

    /**
     * 解锁惊喜彩蛋
     * POST /api/surprises/{id}/unlock
     */
    @PostMapping("/{id}/unlock")
    public Result<SurpriseVO> unlockSurprise(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        String password = null;
        if (body != null) {
            password = body.get("password");
        }

        SurpriseVO vo = surpriseService.unlockSurprise(id, userId, coupleId, password);
        return Result.success("解锁成功", vo);
    }

    /**
     * 获取惊喜彩蛋详情
     * GET /api/surprises/{id}
     */
    @GetMapping("/{id}")
    public Result<SurpriseVO> getSurpriseDetail(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        SurpriseVO vo = surpriseService.getSurpriseDetail(id, coupleId);
        return Result.success(vo);
    }

    /**
     * 删除惊喜彩蛋（仅创建者）
     * DELETE /api/surprises/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteSurprise(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        surpriseService.deleteSurprise(id, userId, coupleId);
        return Result.success("删除成功", null);
    }
}
