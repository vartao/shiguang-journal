package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.PlanDTO;
import com.qinglv.entity.Plan;
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
    private final CurrentUserContext currentUser;

    @GetMapping
    public Result<List<Plan>> getPlans() {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(planService.getPlans(coupleId));
    }

    @PostMapping
    public Result<Plan> createPlan(@Valid @RequestBody PlanDTO dto) {
        Long userId = currentUser.getUserId();
        return Result.success("创建成功", planService.createPlan(userId, currentUser.getCoupleId(), dto));
    }

    @GetMapping("/{id}")
    public Result<Plan> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id, currentUser.getCoupleId()));
    }

    @PutMapping("/{id}")
    public Result<Plan> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanDTO dto) {
        return Result.success("更新成功", planService.updatePlan(id, currentUser.getCoupleId(), dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id, currentUser.getCoupleId());
        return Result.success("删除成功", null);
    }
}
