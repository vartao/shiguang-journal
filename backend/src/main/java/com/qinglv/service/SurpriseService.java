package com.qinglv.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.SurpriseDTO;
import com.qinglv.dto.SurpriseVO;
import com.qinglv.entity.Surprise;
import com.qinglv.entity.User;
import com.qinglv.mapper.SurpriseMapper;
import com.qinglv.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 惊喜彩蛋服务 - 惊喜彩蛋管理、解锁逻辑
 */
@Service
@RequiredArgsConstructor
public class SurpriseService {

    private final SurpriseMapper surpriseMapper;
    private final UserMapper userMapper;

    /**
     * 获取空间下所有惊喜彩蛋
     */
    public List<SurpriseVO> getSurprises(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        List<Surprise> list = surpriseMapper.selectList(
                new LambdaQueryWrapper<Surprise>()
                        .eq(Surprise::getCoupleId, coupleId)
                        .orderByDesc(Surprise::getCreateTime)
        );

        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 获取惊喜彩蛋详情
     */
    public SurpriseVO getSurpriseDetail(Long surpriseId, Long coupleId) {
        Surprise surprise = surpriseMapper.selectById(surpriseId);
        if (surprise == null) {
            throw new BusinessException("彩蛋不存在");
        }
        if (!surprise.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权查看该彩蛋");
        }

        // 检查date类型是否到期自动解锁
        checkAutoUnlock(surprise);

        return convertToVO(surprise);
    }

    /**
     * 创建惊喜彩蛋
     */
    public SurpriseVO createSurprise(Long userId, Long coupleId, SurpriseDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        // 校验解锁条件
        validateUnlockCondition(dto);

        Surprise surprise = new Surprise();
        surprise.setCoupleId(coupleId);
        surprise.setUserId(userId);
        surprise.setTitle(dto.getTitle());
        surprise.setContent(dto.getContent());
        surprise.setHint(dto.getHint());
        surprise.setCoverImage(dto.getCoverImage());
        surprise.setUnlockCondition(dto.getUnlockCondition());
        surprise.setUnlocked(0);

        // 设置解锁条件相关字段
        if ("date".equals(dto.getUnlockCondition())) {
            surprise.setUnlockDate(LocalDate.parse(dto.getUnlockDate(), DateTimeFormatter.ISO_LOCAL_DATE));
        } else if ("password".equals(dto.getUnlockCondition())) {
            // 密码加密存储
            surprise.setUnlockPassword(BCrypt.hashpw(dto.getUnlockPassword()));
        }

        surpriseMapper.insert(surprise);
        return convertToVO(surprise);
    }

    /**
     * 解锁惊喜彩蛋
     *
     * @param surpriseId 彩蛋ID
     * @param userId     解锁者ID
     * @param coupleId   空间ID
     * @param password   密码（password类型时需要）
     */
    @Transactional(rollbackFor = Exception.class)
    public SurpriseVO unlockSurprise(Long surpriseId, Long userId, Long coupleId, String password) {
        Surprise surprise = surpriseMapper.selectById(surpriseId);
        if (surprise == null) {
            throw new BusinessException("彩蛋不存在");
        }
        if (!surprise.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权操作该彩蛋");
        }

        // 已解锁则直接返回
        if (surprise.getUnlocked() == 1) {
            return convertToVO(surprise);
        }

        // 根据解锁条件类型判断
        String condition = surprise.getUnlockCondition();
        switch (condition) {
            case "date":
                // 到期自动解锁：检查是否到期
                if (surprise.getUnlockDate() == null || LocalDate.now().isBefore(surprise.getUnlockDate())) {
                    throw new BusinessException("彩蛋尚未到期，还不能解锁哦~");
                }
                break;

            case "password":
                // 密码解锁：验证密码
                if (StrUtil.isBlank(password)) {
                    throw new BusinessException("请输入解锁密码");
                }
                if (!BCrypt.checkpw(password, surprise.getUnlockPassword())) {
                    throw new BusinessException("密码错误，再试试吧~");
                }
                break;

            case "click":
                // 点击解锁：直接解锁，无需额外验证
                break;

            default:
                throw new BusinessException("未知的解锁条件类型");
        }

        // 执行解锁
        surprise.setUnlocked(1);
        surprise.setUnlockTime(LocalDateTime.now());
        surprise.setUnlockedBy(userId);
        surpriseMapper.updateById(surprise);

        return convertToVO(surprise);
    }

    /**
     * 删除惊喜彩蛋（仅创建者可删）
     */
    public void deleteSurprise(Long surpriseId, Long userId, Long coupleId) {
        Surprise surprise = surpriseMapper.selectById(surpriseId);
        if (surprise == null) {
            throw new BusinessException("彩蛋不存在");
        }
        if (!surprise.getCoupleId().equals(coupleId)) {
            throw new BusinessException(403, "无权操作该彩蛋");
        }
        if (!surprise.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能删除自己创建的惊喜");
        }
        surpriseMapper.deleteById(surpriseId);
    }

    /**
     * 检查 date 类型彩蛋是否到期；到期只标记状态，不伪造「创建者解锁」身份。
     * unlockedBy 留空，表示系统按日期自动开放，避免与真实解锁人混淆。
     */
    private void checkAutoUnlock(Surprise surprise) {
        if ("date".equals(surprise.getUnlockCondition())
                && surprise.getUnlocked() != null
                && surprise.getUnlocked() == 0
                && surprise.getUnlockDate() != null
                && !LocalDate.now().isBefore(surprise.getUnlockDate())) {
            surprise.setUnlocked(1);
            surprise.setUnlockTime(LocalDateTime.now());
            // 日期到期自动开放：不写入 unlockedBy（非用户主动解锁）
            surprise.setUnlockedBy(null);
            surpriseMapper.updateById(surprise);
        }
    }

    /**
     * 校验解锁条件参数
     */
    private void validateUnlockCondition(SurpriseDTO dto) {
        switch (dto.getUnlockCondition()) {
            case "date":
                if (StrUtil.isBlank(dto.getUnlockDate())) {
                    throw new BusinessException("请设置解锁日期");
                }
                break;
            case "password":
                if (StrUtil.isBlank(dto.getUnlockPassword())) {
                    throw new BusinessException("请设置解锁密码");
                }
                break;
            case "click":
                // 点击解锁无需额外参数
                break;
            default:
                throw new BusinessException("不支持的解锁条件类型，可选：date、password、click");
        }
    }

    /**
     * 将Surprise实体转换为SurpriseVO
     */
    private SurpriseVO convertToVO(Surprise surprise) {
        SurpriseVO vo = new SurpriseVO();
        vo.setId(surprise.getId());
        vo.setCoupleId(surprise.getCoupleId());
        vo.setUserId(surprise.getUserId());
        vo.setTitle(surprise.getTitle());
        vo.setHint(surprise.getHint());
        vo.setCoverImage(surprise.getCoverImage());
        vo.setUnlockCondition(surprise.getUnlockCondition());
        vo.setUnlockDate(surprise.getUnlockDate());
        vo.setUnlocked(surprise.getUnlocked());
        vo.setUnlockTime(surprise.getUnlockTime());
        vo.setUnlockedBy(surprise.getUnlockedBy());
        vo.setCreateTime(surprise.getCreateTime());

        // 只有解锁后才返回内容
        if (surprise.getUnlocked() == 1) {
            vo.setContent(surprise.getContent());
        }

        // 查询创建者信息
        User creator = userMapper.selectById(surprise.getUserId());
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
            vo.setFromName(creator.getNickname());
        }
        vo.setCreatedAt(surprise.getCreateTime());

        return vo;
    }
}
