package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.entity.CoupleFeature;
import com.qinglv.mapper.CoupleFeatureMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private CoupleFeatureMapper featureMapper;

    @InjectMocks
    private FeatureService featureService;

    @Test
    void getFeaturesDefaultsAllOnWhenNoRows() {
        when(featureMapper.selectList(any())).thenReturn(List.of());

        Map<String, Boolean> features = featureService.getFeatures(100L);

        assertEquals(FeatureService.FEATURE_KEYS.size(), features.size());
        assertTrue(features.values().stream().allMatch(Boolean::booleanValue));
    }

    @Test
    void getFeaturesWithoutCoupleReturnsAllOnWithoutQuery() {
        Map<String, Boolean> features = featureService.getFeatures(null);

        assertTrue(features.values().stream().allMatch(Boolean::booleanValue));
        verify(featureMapper, never()).selectList(any());
    }

    @Test
    void getFeaturesAppliesStoredOverrides() {
        CoupleFeature off = new CoupleFeature();
        off.setCoupleId(100L);
        off.setFeatureKey("menu");
        off.setEnabled(0);
        when(featureMapper.selectList(any())).thenReturn(List.of(off));

        Map<String, Boolean> features = featureService.getFeatures(100L);

        assertFalse(features.get("menu"));
        assertTrue(features.get("notes"));
    }

    @Test
    void updateFeatureInsertsNewRow() {
        when(featureMapper.selectOne(any())).thenReturn(null);
        when(featureMapper.selectList(any())).thenReturn(List.of());

        Map<String, Boolean> features = featureService.updateFeature(100L, "travels", false);

        verify(featureMapper).insert(any(CoupleFeature.class));
        verify(featureMapper, never()).updateById(any(CoupleFeature.class));
        assertNotNull(features);
    }

    @Test
    void updateFeatureUpdatesExistingRow() {
        CoupleFeature row = new CoupleFeature();
        row.setId(1L);
        row.setCoupleId(100L);
        row.setFeatureKey("menu");
        row.setEnabled(1);
        when(featureMapper.selectOne(any())).thenReturn(row);
        when(featureMapper.selectList(any())).thenReturn(List.of(row));

        featureService.updateFeature(100L, "menu", false);

        assertEquals(0, row.getEnabled());
        verify(featureMapper).updateById(row);
        verify(featureMapper, never()).insert(any(CoupleFeature.class));
    }

    @Test
    void updateFeatureRejectsUnknownKey() {
        assertThrows(BusinessException.class, () -> featureService.updateFeature(100L, "hack", true));
        verify(featureMapper, never()).insert(any(CoupleFeature.class));
    }
}
