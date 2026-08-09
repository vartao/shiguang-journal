package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.CoupleLogDTO;
import com.qinglv.entity.CoupleLog;
import com.qinglv.mapper.CoupleLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoupleLogServiceTest {

    @Mock
    private CoupleLogMapper coupleLogMapper;

    @InjectMocks
    private CoupleLogService coupleLogService;

    private CoupleLogDTO dto;

    @BeforeEach
    void setUp() {
        dto = new CoupleLogDTO();
        dto.setTitle("一起看电影");
        dto.setCategory("movie");
        dto.setLogDate(LocalDate.of(2026, 8, 1));
        dto.setNote("散场后走了很久");
        dto.setRating(5);
    }

    @Test
    void createLogPersistsAllFields() {
        CoupleLog created = coupleLogService.createLog(1L, 100L, dto);

        assertEquals(100L, created.getCoupleId());
        assertEquals(1L, created.getUserId());
        assertEquals("movie", created.getCategory());
        assertEquals(5, created.getRating());
        assertEquals(LocalDate.of(2026, 8, 1), created.getLogDate());
        verify(coupleLogMapper).insert(created);
    }

    @Test
    void createLogWithRatingOutOfRangeIsRejected() {
        dto.setRating(6);

        BusinessException error = assertThrows(
                BusinessException.class, () -> coupleLogService.createLog(1L, 100L, dto));

        assertEquals("评分要在 0-5 之间", error.getMessage());
        verify(coupleLogMapper, never()).insert(any(CoupleLog.class));
    }

    @Test
    void createLogWithNegativeRatingIsRejected() {
        dto.setRating(-1);

        BusinessException error = assertThrows(
                BusinessException.class, () -> coupleLogService.createLog(1L, 100L, dto));

        assertEquals("评分要在 0-5 之间", error.getMessage());
        verify(coupleLogMapper, never()).insert(any(CoupleLog.class));
    }

    @Test
    void createLogWithUnknownCategoryFallsBackToOther() {
        dto.setCategory("unknown");

        CoupleLog created = coupleLogService.createLog(1L, 100L, dto);

        assertEquals("other", created.getCategory());
        verify(coupleLogMapper).insert(created);
    }

    @Test
    void createLogWithoutCoupleIsRejected() {
        BusinessException error = assertThrows(
                BusinessException.class, () -> coupleLogService.createLog(1L, null, dto));

        assertEquals("尚未加入情侣空间", error.getMessage());
        verify(coupleLogMapper, never()).insert(any(CoupleLog.class));
    }

    @Test
    void updateLogFromAnotherCoupleIsRejectedWithoutMutation() {
        CoupleLog existing = new CoupleLog();
        existing.setId(10L);
        existing.setCoupleId(999L);
        when(coupleLogMapper.selectById(10L)).thenReturn(existing);

        BusinessException error = assertThrows(
                BusinessException.class, () -> coupleLogService.updateLog(10L, 1L, 100L, dto));

        assertEquals(403, error.getCode());
        verify(coupleLogMapper, never()).updateById(any(CoupleLog.class));
    }

    @Test
    void updateMissingLogIsRejected() {
        when(coupleLogMapper.selectById(10L)).thenReturn(null);

        BusinessException error = assertThrows(
                BusinessException.class, () -> coupleLogService.updateLog(10L, 1L, 100L, dto));

        assertEquals("记录不存在", error.getMessage());
    }

    @Test
    void deleteLogFromAnotherCoupleIsRejectedWithoutMutation() {
        CoupleLog existing = new CoupleLog();
        existing.setId(10L);
        existing.setCoupleId(999L);
        when(coupleLogMapper.selectById(10L)).thenReturn(existing);

        BusinessException error = assertThrows(
                BusinessException.class, () -> coupleLogService.deleteLog(10L, 100L));

        assertEquals(403, error.getCode());
        verify(coupleLogMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteLogWithinCoupleSucceeds() {
        CoupleLog existing = new CoupleLog();
        existing.setId(10L);
        existing.setCoupleId(100L);
        when(coupleLogMapper.selectById(10L)).thenReturn(existing);

        coupleLogService.deleteLog(10L, 100L);

        verify(coupleLogMapper).deleteById(10L);
    }
}
