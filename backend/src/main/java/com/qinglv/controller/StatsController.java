package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<Map<String, Object>> getStats() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;
        return Result.success(statsService.getCoupleStats(coupleId, userId));
    }
}
