package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.PlanDTO;
import com.qinglv.entity.Plan;
import com.qinglv.mapper.PlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanMapper planMapper;

    @InjectMocks
    private PlanService planService;

    private Plan plan;

    @BeforeEach
    void setUp() {
        plan = new Plan();
        plan.setId(10L);
        plan.setCoupleId(100L);
        plan.setUserId(1L);
        plan.setTitle("海边之旅");
        plan.setStatus(0);
    }

    @Test
    void createPlanPersistsWithDefaults() {
        PlanDTO dto = new PlanDTO();
        dto.setTitle("海边之旅");
        dto.setDestination("青岛");

        Plan created = planService.createPlan(1L, 100L, dto);

        assertEquals("海边之旅", created.getTitle());
        assertEquals(0, created.getStatus());
        assertEquals(100L, created.getCoupleId());
        verify(planMapper).insert(created);
    }

    @Test
    void createPlanWithoutCoupleIsRejected() {
        PlanDTO dto = new PlanDTO();
        dto.setTitle("海边之旅");

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.createPlan(1L, null, dto));

        assertEquals("尚未加入情侣空间", error.getMessage());
        verify(planMapper, never()).insert(any(Plan.class));
    }

    @Test
    void createPlanWithInvalidDaysJsonIsRejected() {
        PlanDTO dto = new PlanDTO();
        dto.setTitle("海边之旅");
        dto.setDays("{not valid json");

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.createPlan(1L, 100L, dto));

        assertEquals("每日安排格式不正确", error.getMessage());
        verify(planMapper, never()).insert(any(Plan.class));
    }

    @Test
    void createPlanWithValidDaysJsonIsAccepted() {
        PlanDTO dto = new PlanDTO();
        dto.setTitle("海边之旅");
        dto.setDays("[{\"date\":\"2026-08-10\",\"items\":[\"10:00 高铁出发\"]}]");

        Plan created = planService.createPlan(1L, 100L, dto);

        assertEquals(dto.getDays(), created.getDays());
        verify(planMapper).insert(created);
    }

    @Test
    void viewingPlanFromAnotherCoupleIsRejected() {
        when(planMapper.selectById(10L)).thenReturn(plan);

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.getPlan(10L, 999L));

        assertEquals(403, error.getCode());
    }

    @Test
    void updatingPlanFromAnotherCoupleIsRejectedWithoutMutation() {
        when(planMapper.selectById(10L)).thenReturn(plan);

        PlanDTO dto = new PlanDTO();
        dto.setTitle("改名");

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.updatePlan(10L, 999L, dto));

        assertEquals(403, error.getCode());
        verify(planMapper, never()).updateById(any(Plan.class));
    }

    @Test
    void updatePlanWithInvalidDaysJsonIsRejected() {
        when(planMapper.selectById(10L)).thenReturn(plan);

        PlanDTO dto = new PlanDTO();
        dto.setTitle("海边之旅");
        dto.setDays("[broken");

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.updatePlan(10L, 100L, dto));

        assertEquals("每日安排格式不正确", error.getMessage());
        verify(planMapper, never()).updateById(any(Plan.class));
    }

    @Test
    void deletingMissingPlanIsRejected() {
        when(planMapper.selectById(10L)).thenReturn(null);

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.deletePlan(10L, 100L));

        assertEquals("行程不存在", error.getMessage());
        verify(planMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deletingPlanFromAnotherCoupleIsRejectedWithoutMutation() {
        when(planMapper.selectById(10L)).thenReturn(plan);

        BusinessException error = assertThrows(
                BusinessException.class, () -> planService.deletePlan(10L, 999L));

        assertEquals(403, error.getCode());
        verify(planMapper, never()).deleteById(any(Long.class));
    }
}
