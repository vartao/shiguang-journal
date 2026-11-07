package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final CurrentUserContext currentUser;

    @GetMapping
    public Result<Map<String, Object>> getStats() {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success(statsService.getCoupleStats(coupleId, userId));
    }

    /**
     * 那年今天
     * GET /api/stats/on-this-day
     */
    @GetMapping("/on-this-day")
    public Result<List<Map<String, Object>>> getOnThisDay() {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(statsService.getOnThisDay(coupleId));
    }
}
