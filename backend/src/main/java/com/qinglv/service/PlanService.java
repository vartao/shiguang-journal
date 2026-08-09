package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.PlanDTO;
import com.qinglv.entity.Plan;
import com.qinglv.mapper.PlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 行程计划服务 - 出发前的行程管理
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PlanMapper planMapper;

    /**
     * 获取空间下所有行程（创建时间倒序）
     */
    public List<Plan> getPlans(Long coupleId) {
        requireCouple(coupleId);
        return planMapper.selectList(
                new LambdaQueryWrapper<Plan>()
                        .eq(Plan::getCoupleId, coupleId)
                        .orderByDesc(Plan::getCreateTime)
        );
    }

    /**
     * 获取单个行程
     */
    public Plan getPlan(Long planId, Long coupleId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("行程不存在");
        }
        if (!Objects.equals(plan.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权查看该行程");
        }
        return plan;
    }

    /**
     * 创建行程
     */
    public Plan createPlan(Long userId, Long coupleId, PlanDTO dto) {
        requireCouple(coupleId);
        validateDays(dto.getDays());

        Plan plan = new Plan();
        plan.setCoupleId(coupleId);
        plan.setUserId(userId);
        plan.setTitle(dto.getTitle().trim());
        plan.setDestination(dto.getDestination());
        plan.setCover(dto.getCover());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setBudget(dto.getBudget());
        plan.setDays(dto.getDays());
        plan.setNote(dto.getNote());
        plan.setStatus(dto.getStatus() != null ? dto.getStatus() : 0);

        planMapper.insert(plan);
        return plan;
    }

    /**
     * 更新行程
     */
    public Plan updatePlan(Long planId, Long coupleId, PlanDTO dto) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("行程不存在");
        }
        if (!Objects.equals(plan.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权修改该行程");
        }
        validateDays(dto.getDays());

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            plan.setTitle(dto.getTitle().trim());
        }
        if (dto.getDestination() != null) plan.setDestination(dto.getDestination());
        if (dto.getCover() != null) plan.setCover(dto.getCover());
        if (dto.getStartDate() != null) plan.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) plan.setEndDate(dto.getEndDate());
        if (dto.getBudget() != null) plan.setBudget(dto.getBudget());
        if (dto.getDays() != null) plan.setDays(dto.getDays());
        if (dto.getNote() != null) plan.setNote(dto.getNote());
        if (dto.getStatus() != null) plan.setStatus(dto.getStatus());

        planMapper.updateById(plan);
        return plan;
    }

    /**
     * 删除行程
     */
    public void deletePlan(Long planId, Long coupleId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException("行程不存在");
        }
        if (!Objects.equals(plan.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该行程");
        }
        planMapper.deleteById(planId);
    }

    private void requireCouple(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
    }

    /**
     * days 非空时必须是合法 JSON
     */
    private void validateDays(String days) {
        if (days == null || days.isBlank()) {
            return;
        }
        try {
            OBJECT_MAPPER.readTree(days);
        } catch (Exception e) {
            throw new BusinessException("每日安排格式不正确");
        }
    }
}
