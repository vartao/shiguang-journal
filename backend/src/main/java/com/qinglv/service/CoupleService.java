package com.qinglv.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.CoupleUpdateDTO;
import com.qinglv.dto.CoupleVO;
import com.qinglv.entity.Couple;
import com.qinglv.entity.User;
import com.qinglv.mapper.CoupleMapper;
import com.qinglv.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * 情侣空间服务 - 空间管理、计算在一起天数
 */
@Service
@RequiredArgsConstructor
public class CoupleService {

    private final CoupleMapper coupleMapper;
    private final UserMapper userMapper;

    /**
     * 获取情侣空间信息（含在一起天数）
     */
    public CoupleVO getCoupleInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Couple couple = coupleMapper.selectById(user.getCoupleId());
        if (couple == null) {
            throw new BusinessException("情侣空间不存在");
        }

        return buildCoupleVO(couple, userId);
    }

    /**
     * 创建情侣空间（用户尚未加入任何空间时）
     */
    @Transactional(rollbackFor = Exception.class)
    public CoupleVO createCouple(Long userId, Map<String, String> body) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getCoupleId() != null) {
            throw new BusinessException("你已在情侣空间中，无需重复创建");
        }

        String name = body != null ? body.get("name") : null;
        String dateStr = null;
        if (body != null) {
            dateStr = StrUtil.blankToDefault(body.get("togetherDate"), body.get("startDate"));
        }

        Couple couple = new Couple();
        couple.setName(StrUtil.isNotBlank(name) ? name : user.getNickname() + "的小窝");
        couple.setUserId1(userId);
        couple.setInviteCode(generateInviteCode());
        couple.setAnnouncement("欢迎回家~");
        if (StrUtil.isNotBlank(dateStr)) {
            couple.setTogetherDate(LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE));
        }
        coupleMapper.insert(couple);

        user.setCoupleId(couple.getId());
        user.setRole("owner");
        userMapper.updateById(user);

        return buildCoupleVO(couple, userId);
    }

    /**
     * 通过邀请码绑定伴侣
     */
    @Transactional(rollbackFor = Exception.class)
    public CoupleVO bindPartner(Long userId, String inviteCode) {
        if (StrUtil.isBlank(inviteCode)) {
            throw new BusinessException("请输入邀请码");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getCoupleId() != null) {
            throw new BusinessException("你已经有小窝了，不能再绑定另一个邀请码");
        }

        Couple couple = coupleMapper.selectByInviteCodeForUpdate(inviteCode.trim().toUpperCase());
        if (couple == null) {
            throw new BusinessException("邀请码不正确，请向 TA 再确认一次～");
        }
        if (couple.getUserId1() != null && couple.getUserId2() != null) {
            throw new BusinessException("这个小窝已经住满两人啦，无法再加入");
        }
        if (userId.equals(couple.getUserId1()) || userId.equals(couple.getUserId2())) {
            throw new BusinessException("你已经在这个小窝里啦");
        }

        if (couple.getUserId1() == null) {
            couple.setUserId1(userId);
        } else {
            couple.setUserId2(userId);
        }
        coupleMapper.updateById(couple);

        user.setCoupleId(couple.getId());
        user.setRole("member");
        userMapper.updateById(user);

        return buildCoupleVO(couple, userId);
    }

    /**
     * 更新情侣空间信息
     */
    public CoupleVO updateCoupleInfo(Long userId, CoupleUpdateDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getCoupleId() == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Couple couple = coupleMapper.selectById(user.getCoupleId());
        if (couple == null) {
            throw new BusinessException("情侣空间不存在");
        }

        if (StrUtil.isNotBlank(dto.getName())) {
            couple.setName(dto.getName());
        }
        if (dto.getAvatar() != null) {
            couple.setAvatar(dto.getAvatar());
        }
        if (StrUtil.isNotBlank(dto.getTogetherDate())) {
            couple.setTogetherDate(LocalDate.parse(dto.getTogetherDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        }
        if (dto.getAnnouncement() != null) {
            couple.setAnnouncement(dto.getAnnouncement());
        }

        coupleMapper.updateById(couple);
        return buildCoupleVO(couple, userId);
    }

    public Long calculateTogetherDays(LocalDate togetherDate) {
        if (togetherDate == null) {
            return 0L;
        }
        return ChronoUnit.DAYS.between(togetherDate, LocalDate.now());
    }

    private CoupleVO buildCoupleVO(Couple couple, Long currentUserId) {
        CoupleVO vo = new CoupleVO();
        vo.setId(couple.getId());
        vo.setName(couple.getName());
        vo.setAvatar(couple.getAvatar());
        vo.setTogetherDate(couple.getTogetherDate());
        vo.setTogetherDays(calculateTogetherDays(couple.getTogetherDate()));
        vo.setUserId1(couple.getUserId1());
        vo.setUserId2(couple.getUserId2());
        vo.setInviteCode(couple.getInviteCode());
        vo.setAnnouncement(couple.getAnnouncement());
        vo.setCreateTime(couple.getCreateTime());
        // 前端兼容字段
        vo.setStartDate(couple.getTogetherDate());

        if (couple.getUserId1() != null) {
            User user1 = userMapper.selectById(couple.getUserId1());
            if (user1 != null) {
                vo.setUser1Nickname(user1.getNickname());
                vo.setUser1Avatar(user1.getAvatar());
            }
        }
        if (couple.getUserId2() != null) {
            User user2 = userMapper.selectById(couple.getUserId2());
            if (user2 != null) {
                vo.setUser2Nickname(user2.getNickname());
                vo.setUser2Avatar(user2.getAvatar());
            }
        }

        // partnerName：相对当前用户的另一半昵称
        if (currentUserId != null) {
            if (currentUserId.equals(couple.getUserId1())) {
                vo.setPartnerName(vo.getUser2Nickname());
            } else if (currentUserId.equals(couple.getUserId2())) {
                vo.setPartnerName(vo.getUser1Nickname());
            }
        }
        if (vo.getPartnerName() == null) {
            vo.setPartnerName(vo.getUser2Nickname() != null ? vo.getUser2Nickname() : vo.getUser1Nickname());
        }

        return vo;
    }

    private String generateInviteCode() {
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        Long count = coupleMapper.selectCount(
                new LambdaQueryWrapper<Couple>().eq(Couple::getInviteCode, code)
        );
        if (count > 0) {
            return generateInviteCode();
        }
        return code;
    }
}
