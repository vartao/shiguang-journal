package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.entity.CoupleFeature;
import com.qinglv.mapper.CoupleFeatureMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 功能开关服务 - 按情侣空间控制功能入口显示/隐藏
 */
@Service
@RequiredArgsConstructor
public class FeatureService {

    /** 支持开关的功能 key（顺序即导航展示顺序），默认全部开启 */
    public static final List<String> FEATURE_KEYS = List.of(
            "notes", "albums", "anniversary", "surprise", "wishes", "stats", "travels", "plans", "menu",
            "capsule", "logs", "board");

    private static final Set<String> KEY_SET = Set.copyOf(FEATURE_KEYS);

    private final CoupleFeatureMapper featureMapper;

    /**
     * 获取空间的功能开关 map；无记录的功能默认开启。
     * 未加入空间（coupleId 为 null）时返回全部开启，保证导航可用。
     */
    public Map<String, Boolean> getFeatures(Long coupleId) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        FEATURE_KEYS.forEach(key -> result.put(key, true));
        if (coupleId == null) {
            return result;
        }
        List<CoupleFeature> rows = featureMapper.selectList(
                new LambdaQueryWrapper<CoupleFeature>().eq(CoupleFeature::getCoupleId, coupleId));
        for (CoupleFeature row : rows) {
            if (KEY_SET.contains(row.getFeatureKey())) {
                result.put(row.getFeatureKey(), row.getEnabled() != null && row.getEnabled() == 1);
            }
        }
        return result;
    }

    /**
     * 更新某个功能开关（upsert），返回最新全量 map
     */
    public Map<String, Boolean> updateFeature(Long coupleId, String featureKey, boolean enabled) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        if (!KEY_SET.contains(featureKey)) {
            throw new BusinessException("未知的功能项：" + featureKey);
        }

        CoupleFeature row = featureMapper.selectOne(
                new LambdaQueryWrapper<CoupleFeature>()
                        .eq(CoupleFeature::getCoupleId, coupleId)
                        .eq(CoupleFeature::getFeatureKey, featureKey));
        if (row == null) {
            row = new CoupleFeature();
            row.setCoupleId(coupleId);
            row.setFeatureKey(featureKey);
            row.setEnabled(enabled ? 1 : 0);
            featureMapper.insert(row);
        } else {
            row.setEnabled(enabled ? 1 : 0);
            featureMapper.updateById(row);
        }
        return getFeatures(coupleId);
    }
}
