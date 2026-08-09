package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.PlanDTO;
import com.qinglv.entity.Plan;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<List<Plan>> getPlans() {
        Long coupleId = currentCoupleId();
        return Result.success(planService.getPlans(coupleId));
    }

    @PostMapping
    public Result<Plan> createPlan(@Valid @RequestBody PlanDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        return Result.success("创建成功", planService.createPlan(userId, currentCoupleId(), dto));
    }

    @GetMapping("/{id}")
    public Result<Plan> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id, currentCoupleId()));
    }

    @PutMapping("/{id}")
    public Result<Plan> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanDTO dto) {
        return Result.success("更新成功", planService.updatePlan(id, currentCoupleId(), dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id, currentCoupleId());
        return Result.success("删除成功", null);
    }

    private Long currentCoupleId() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return user != null ? user.getCoupleId() : null;
    }
}
