package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.CoupleLogDTO;
import com.qinglv.entity.CoupleLog;
import com.qinglv.mapper.CoupleLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 小事记（情侣日志）：双方共享，都可记、可改、可删，仅按 couple 隔离
 */
@Service
@RequiredArgsConstructor
public class CoupleLogService {

    private static final Set<String> CATEGORIES =
            Set.of("movie", "food", "show", "travel", "date", "sport", "other");

    private final CoupleLogMapper coupleLogMapper;

    /**
     * 获取小事记列表，按发生日期倒序；category/year 可选过滤
     */
    public List<CoupleLog> getLogList(Long coupleId, String category, Integer year) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        LambdaQueryWrapper<CoupleLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CoupleLog::getCoupleId, coupleId);
        if (StringUtils.hasText(category) && CATEGORIES.contains(category)) {
            wrapper.eq(CoupleLog::getCategory, category);
        }
        if (year != null) {
            wrapper.apply("YEAR(log_date) = {0}", year);
        }
        wrapper.orderByDesc(CoupleLog::getLogDate)
               .orderByDesc(CoupleLog::getId);
        return coupleLogMapper.selectList(wrapper);
    }

    /**
     * 创建小事记
     */
    public CoupleLog createLog(Long userId, Long coupleId, CoupleLogDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        CoupleLog log = new CoupleLog();
        log.setCoupleId(coupleId);
        log.setUserId(userId);
        log.setTitle(dto.getTitle());
        log.setCategory(normalizeCategory(dto.getCategory()));
        log.setLogDate(dto.getLogDate());
        log.setNote(dto.getNote());
        log.setCover(dto.getCover());
        log.setRating(normalizeRating(dto.getRating()));

        coupleLogMapper.insert(log);
        return log;
    }

    /**
     * 更新小事记（双方都可修改，仅校验 couple 隔离）
     */
    public CoupleLog updateLog(Long logId, Long userId, Long coupleId, CoupleLogDTO dto) {
        CoupleLog log = coupleLogMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException("记录不存在");
        }
        if (!Objects.equals(log.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权修改该记录");
        }

        log.setTitle(dto.getTitle());
        log.setCategory(normalizeCategory(dto.getCategory()));
        log.setLogDate(dto.getLogDate());
        log.setNote(dto.getNote());
        log.setCover(dto.getCover());
        log.setRating(normalizeRating(dto.getRating()));

        coupleLogMapper.updateById(log);
        return log;
    }

    /**
     * 删除小事记（双方都可删除，仅校验 couple 隔离）
     */
    public void deleteLog(Long logId, Long coupleId) {
        CoupleLog log = coupleLogMapper.selectById(logId);
        if (log == null) {
            throw new BusinessException("记录不存在");
        }
        if (!Objects.equals(log.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该记录");
        }
        coupleLogMapper.deleteById(logId);
    }

    /** 分类白名单之外的一律落为 other */
    private String normalizeCategory(String category) {
        return category != null && CATEGORIES.contains(category) ? category : "other";
    }

    /** 评分限定 0-5，空值按 0 处理 */
    private Integer normalizeRating(Integer rating) {
        if (rating == null) {
            return 0;
        }
        if (rating < 0 || rating > 5) {
            throw new BusinessException("评分要在 0-5 之间");
        }
        return rating;
    }
}
