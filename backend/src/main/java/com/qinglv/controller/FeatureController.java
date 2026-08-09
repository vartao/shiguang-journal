package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 功能开关控制器
 */
@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureService featureService;
    private final UserMapper userMapper;

    /**
     * 获取当前空间的功能开关
     * GET /api/features
     */
    @GetMapping
    public Result<Map<String, Boolean>> getFeatures() {
        return Result.success(featureService.getFeatures(currentCoupleId()));
    }

    /**
     * 更新某个功能开关
     * PUT /api/features/{key}  body: {enabled: true}
     */
    @PutMapping("/{key}")
    public Result<Map<String, Boolean>> updateFeature(@PathVariable String key, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body == null || !Boolean.FALSE.equals(body.get("enabled"));
        return Result.success("已更新", featureService.updateFeature(currentCoupleId(), key, enabled));
    }

    private Long currentCoupleId() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return user != null ? user.getCoupleId() : null;
    }
}
