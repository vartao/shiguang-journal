package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.AnniversaryDTO;
import com.qinglv.service.AnniversaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 纪念日控制器
 */
@RestController
@RequestMapping("/api/anniversaries")
@RequiredArgsConstructor
public class AnniversaryController {

    private final AnniversaryService anniversaryService;
    private final CurrentUserContext currentUser;

    /**
     * 获取所有纪念日（含倒计时）
     * GET /api/anniversaries
     */
    @GetMapping
    public Result<List<Map<String, Object>>> getAnniversaries() {
        Long coupleId = currentUser.getCoupleId();

        List<Map<String, Object>> list = anniversaryService.getAnniversaries(coupleId);
        return Result.success(list);
    }

    /**
     * 创建纪念日
     * POST /api/anniversaries
     */
    @PostMapping
    public Result<Map<String, Object>> createAnniversary(@Valid @RequestBody AnniversaryDTO dto) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();

        Map<String, Object> result = anniversaryService.createAnniversary(userId, coupleId, dto);
        return Result.success("创建成功", result);
    }

    /**
     * 更新纪念日
     * PUT /api/anniversaries/{id}
     */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateAnniversary(@PathVariable Long id, @Valid @RequestBody AnniversaryDTO dto) {
        Long coupleId = currentUser.getCoupleId();

        Map<String, Object> result = anniversaryService.updateAnniversary(id, coupleId, dto);
        return Result.success("更新成功", result);
    }

    /**
     * 删除纪念日
     * DELETE /api/anniversaries/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAnniversary(@PathVariable Long id) {
        Long coupleId = currentUser.getCoupleId();

        anniversaryService.deleteAnniversary(id, coupleId);
        return Result.success("删除成功", null);
    }
}
