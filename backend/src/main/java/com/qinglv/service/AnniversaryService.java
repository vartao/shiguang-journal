package com.qinglv.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.AnniversaryDTO;
import com.qinglv.entity.Anniversary;
import com.qinglv.mapper.AnniversaryMapper;
import com.qinglv.service.support.AnniversaryDateCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 纪念日服务 - 纪念日管理、倒计时计算
 */
@Service
@RequiredArgsConstructor
public class AnniversaryService {

    private final AnniversaryMapper anniversaryMapper;

    /**
     * 获取空间下所有纪念日（含倒计时）
     */
    public List<Map<String, Object>> getAnniversaries(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        List<Anniversary> list = anniversaryMapper.selectList(
                new LambdaQueryWrapper<Anniversary>()
                        .eq(Anniversary::getCoupleId, coupleId)
                        .orderByAsc(Anniversary::getDate)
        );

        return list.stream().map(this::buildAnniversaryWithCountdown).collect(Collectors.toList());
    }

    /**
     * 创建纪念日
     */
    public Map<String, Object> createAnniversary(Long userId, Long coupleId, AnniversaryDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Anniversary anniversary = new Anniversary();
        anniversary.setCoupleId(coupleId);
        anniversary.setUserId(userId);
        anniversary.setName(resolveName(dto));
        anniversary.setDate(LocalDate.parse(dto.getDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        anniversary.setType(StrUtil.isNotBlank(dto.getType()) ? dto.getType() : "custom");
        anniversary.setIcon(dto.getIcon());
        anniversary.setRepeatYearly(dto.getRepeatYearly() != null ? dto.getRepeatYearly() : 1);
        anniversary.setRemark(dto.getRemark());

        anniversaryMapper.insert(anniversary);
        return buildAnniversaryWithCountdown(anniversary);
    }

    /**
     * 更新纪念日
     */
    public Map<String, Object> updateAnniversary(Long anniversaryId, Long coupleId, AnniversaryDTO dto) {
        Anniversary anniversary = anniversaryMapper.selectById(anniversaryId);
        if (anniversary == null) {
            throw new BusinessException("纪念日不存在");
        }
        if (!anniversary.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权编辑该纪念日");
        }

        anniversary.setName(resolveName(dto));
        anniversary.setDate(LocalDate.parse(dto.getDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        if (StrUtil.isNotBlank(dto.getType())) {
            anniversary.setType(dto.getType());
        }
        if (dto.getIcon() != null) {
            anniversary.setIcon(dto.getIcon());
        }
        if (dto.getRepeatYearly() != null) {
            anniversary.setRepeatYearly(dto.getRepeatYearly());
        }
        if (dto.getRemark() != null) {
            anniversary.setRemark(dto.getRemark());
        }

        anniversaryMapper.updateById(anniversary);
        return buildAnniversaryWithCountdown(anniversary);
    }

    /**
     * 删除纪念日
     */
    public void deleteAnniversary(Long anniversaryId, Long coupleId) {
        Anniversary anniversary = anniversaryMapper.selectById(anniversaryId);
        if (anniversary == null) {
            throw new BusinessException("纪念日不存在");
        }
        if (!anniversary.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权删除该纪念日");
        }
        anniversaryMapper.deleteById(anniversaryId);
    }

    /**
     * 构建纪念日信息（含倒计时）
     */
    private Map<String, Object> buildAnniversaryWithCountdown(Anniversary anniversary) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", anniversary.getId());
        map.put("coupleId", anniversary.getCoupleId());
        map.put("userId", anniversary.getUserId());
        map.put("name", anniversary.getName());
        // 前端 CountdownCard 使用 title
        map.put("title", anniversary.getName());
        map.put("date", anniversary.getDate());
        map.put("type", anniversary.getType());
        map.put("icon", anniversary.getIcon());
        map.put("repeatYearly", anniversary.getRepeatYearly());
        map.put("remark", anniversary.getRemark());
        map.put("createTime", anniversary.getCreateTime());
        map.put("createdAt", anniversary.getCreateTime());

        LocalDate today = LocalDate.now();
        AnniversaryDateCalculator.Metrics metrics =
                AnniversaryDateCalculator.calculate(anniversary.getDate(), anniversary.getRepeatYearly(), today);
        map.put("daysPassed", metrics.daysPassed());
        map.put("daysUntilNext", metrics.daysUntilNext());
        map.put("years", metrics.years());

        return map;
    }

    private String resolveName(AnniversaryDTO dto) {
        if (StrUtil.isNotBlank(dto.getName())) {
            return dto.getName();
        }
        if (StrUtil.isNotBlank(dto.getTitle())) {
            return dto.getTitle();
        }
        throw new BusinessException("纪念日名称不能为空");
    }

}
