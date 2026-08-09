package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.CoupleLogDTO;
import com.qinglv.entity.CoupleLog;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.CoupleLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class CoupleLogController {

    private final CoupleLogService coupleLogService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<List<CoupleLog>> getLogs(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) Integer year) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;
        return Result.success(coupleLogService.getLogList(coupleId, category, year));
    }

    @PostMapping
    public Result<CoupleLog> createLog(@Valid @RequestBody CoupleLogDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;
        return Result.success("记录成功", coupleLogService.createLog(userId, coupleId, dto));
    }

    @PutMapping("/{id}")
    public Result<CoupleLog> updateLog(@PathVariable Long id, @Valid @RequestBody CoupleLogDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;
        return Result.success("更新成功", coupleLogService.updateLog(id, userId, coupleId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteLog(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;
        coupleLogService.deleteLog(id, coupleId);
        return Result.success("删除成功", null);
    }
}
