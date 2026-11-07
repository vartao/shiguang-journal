package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinglv.common.BusinessException;
import com.qinglv.common.UploadUrls;
import com.qinglv.dto.CapsuleDTO;
import com.qinglv.dto.CapsuleVO;
import com.qinglv.entity.Capsule;
import com.qinglv.entity.User;
import com.qinglv.mapper.CapsuleMapper;
import com.qinglv.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CapsuleService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CapsuleMapper capsuleMapper;
    private final UserMapper userMapper;

    /**
     * 获取胶囊列表（按开启日期升序）。
     * 未开启且未到期的胶囊不返回信件内容和照片，防止提前泄露。
     */
    public List<CapsuleVO> getCapsuleList(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        LambdaQueryWrapper<Capsule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Capsule::getCoupleId, coupleId)
               .orderByAsc(Capsule::getOpenDate)
               .orderByAsc(Capsule::getId);

        List<Capsule> capsules = capsuleMapper.selectList(wrapper);
        return convertToVOs(capsules, LocalDate.now());
    }

    /**
     * 创建胶囊：开启日期必须是未来的日期
     */
    public CapsuleVO createCapsule(Long userId, Long coupleId, CapsuleDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        if (dto.getOpenDate() == null || !dto.getOpenDate().isAfter(LocalDate.now())) {
            throw new BusinessException("开启日期要是未来的某一天");
        }

        Capsule capsule = new Capsule();
        capsule.setCoupleId(coupleId);
        capsule.setUserId(userId);
        capsule.setTitle(dto.getTitle());
        capsule.setMessage(dto.getMessage());
        capsule.setOpenDate(dto.getOpenDate());
        capsule.setPhotoUrls(serializePhotoUrls(dto.getPhotoUrls()));
        capsule.setIsOpened(0);

        capsuleMapper.insert(capsule);
        return convertToVO(capsule, LocalDate.now());
    }

    /**
     * 拆开胶囊（双方都可操作，幂等）
     */
    public CapsuleVO openCapsule(Long capsuleId, Long userId, Long coupleId) {
        Capsule capsule = capsuleMapper.selectById(capsuleId);
        if (capsule == null) {
            throw new BusinessException("胶囊不存在");
        }
        if (!Objects.equals(capsule.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权操作该胶囊");
        }
        if (Objects.equals(capsule.getIsOpened(), 1)) {
            return convertToVO(capsule, LocalDate.now());
        }
        if (capsule.getOpenDate().isAfter(LocalDate.now())) {
            throw new BusinessException("还没到开启的日子，再等等");
        }

        capsule.setIsOpened(1);
        capsule.setOpenedBy(userId);
        capsule.setOpenedTime(LocalDateTime.now());
        capsuleMapper.updateById(capsule);
        return convertToVO(capsule, LocalDate.now());
    }

    /**
     * 删除胶囊（仅创建者）
     */
    public void deleteCapsule(Long capsuleId, Long userId, Long coupleId) {
        Capsule capsule = capsuleMapper.selectById(capsuleId);
        if (capsule == null) {
            throw new BusinessException("胶囊不存在");
        }
        if (!Objects.equals(capsule.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该胶囊");
        }
        if (!Objects.equals(capsule.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己创建的胶囊");
        }
        capsuleMapper.deleteById(capsuleId);
    }

    /**
     * 批量装配胶囊展示数据，避免每条胶囊分别查询创建者/拆开人造成 N+1
     */
    private List<CapsuleVO> convertToVOs(List<Capsule> capsules, LocalDate today) {
        Set<Long> userIds = new HashSet<>();
        for (Capsule capsule : capsules) {
            if (capsule.getUserId() != null) userIds.add(capsule.getUserId());
            if (capsule.getOpenedBy() != null) userIds.add(capsule.getOpenedBy());
        }
        Map<Long, User> usersById = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                usersById.put(user.getId(), user);
            }
        }
        return capsules.stream().map(capsule -> convertToVO(capsule, today, usersById)).collect(Collectors.toList());
    }

    private CapsuleVO convertToVO(Capsule capsule, LocalDate today) {
        return convertToVO(capsule, today, null);
    }

    private CapsuleVO convertToVO(Capsule capsule, LocalDate today, Map<Long, User> usersById) {
        CapsuleVO vo = new CapsuleVO();
        vo.setId(capsule.getId());
        vo.setCoupleId(capsule.getCoupleId());
        vo.setUserId(capsule.getUserId());
        vo.setTitle(capsule.getTitle());
        vo.setOpenDate(capsule.getOpenDate());
        vo.setIsOpened(capsule.getIsOpened());
        vo.setOpenedBy(capsule.getOpenedBy());
        vo.setOpenedTime(capsule.getOpenedTime());
        vo.setCreateTime(capsule.getCreateTime());

        boolean opened = Objects.equals(capsule.getIsOpened(), 1);
        boolean due = !capsule.getOpenDate().isAfter(today);
        vo.setCanOpen(!opened && due);
        vo.setDaysLeft(opened || due ? 0L : ChronoUnit.DAYS.between(today, capsule.getOpenDate()));

        // 未开启且未到期：隐藏内容，列表只展示标题/开启日期/锁定状态
        if (opened || due) {
            vo.setMessage(capsule.getMessage());
            vo.setPhotoUrls(deserializePhotoUrls(capsule.getPhotoUrls()));
        }

        User creator = usersById != null
                ? (capsule.getUserId() == null ? null : usersById.get(capsule.getUserId()))
                : userMapper.selectById(capsule.getUserId());
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
            vo.setCreatorAvatar(creator.getAvatar());
        }
        if (capsule.getOpenedBy() != null) {
            User opener = usersById != null
                    ? usersById.get(capsule.getOpenedBy())
                    : userMapper.selectById(capsule.getOpenedBy());
            if (opener != null) {
                vo.setOpenerNickname(opener.getNickname());
            }
        }
        return vo;
    }

    private String serializePhotoUrls(List<String> photoUrls) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(
                    photoUrls.stream().map(UploadUrls::toStoredPath).toList());
        } catch (Exception e) {
            throw new BusinessException("照片格式不正确");
        }
    }

    private List<String> deserializePhotoUrls(String photoUrls) {
        if (photoUrls == null || photoUrls.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(photoUrls, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
