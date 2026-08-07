package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.WishDTO;
import com.qinglv.dto.WishVO;
import com.qinglv.entity.User;
import com.qinglv.entity.Wish;
import com.qinglv.mapper.UserMapper;
import com.qinglv.mapper.WishMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishService {

    private final WishMapper wishMapper;
    private final UserMapper userMapper;

    /**
     * 获取心愿列表
     */
    public List<WishVO> getWishList(Long coupleId, Integer status) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        LambdaQueryWrapper<Wish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Wish::getCoupleId, coupleId);

        if (status != null) {
            wrapper.eq(Wish::getStatus, status);
        }

        wrapper.orderByDesc(Wish::getPriority)
               .orderByDesc(Wish::getCreateTime);

        List<Wish> wishes = wishMapper.selectList(wrapper);
        return wishes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 创建心愿
     */
    public WishVO createWish(Long userId, Long coupleId, WishDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Wish wish = new Wish();
        wish.setCoupleId(coupleId);
        wish.setUserId(userId);
        wish.setTitle(dto.getTitle());
        wish.setDescription(dto.getDescription());
        wish.setIcon(dto.getIcon());
        wish.setCategory(dto.getCategory() != null ? dto.getCategory() : "other");
        wish.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        wish.setStatus(0);

        wishMapper.insert(wish);
        return convertToVO(wish);
    }

    /**
     * 更新心愿
     */
    public WishVO updateWish(Long wishId, Long userId, Long coupleId, WishDTO dto) {
        Wish wish = wishMapper.selectById(wishId);
        if (wish == null) {
            throw new BusinessException("心愿不存在");
        }
        if (!Objects.equals(wish.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权修改该心愿");
        }
        if (!Objects.equals(wish.getUserId(), userId)) {
            throw new BusinessException(403, "只能修改自己的心愿");
        }

        wish.setTitle(dto.getTitle());
        wish.setDescription(dto.getDescription());
        if (dto.getIcon() != null) wish.setIcon(dto.getIcon());
        if (dto.getCategory() != null) wish.setCategory(dto.getCategory());
        if (dto.getPriority() != null) wish.setPriority(dto.getPriority());

        wishMapper.updateById(wish);
        return convertToVO(wish);
    }

    /**
     * 完成心愿（双方都可操作）
     */
    public WishVO completeWish(Long wishId, Long userId, Long coupleId) {
        Wish wish = wishMapper.selectById(wishId);
        if (wish == null) {
            throw new BusinessException("心愿不存在");
        }
        if (!Objects.equals(wish.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权操作该心愿");
        }

        wish.setStatus(1);
        wish.setCompletedTime(LocalDateTime.now());
        wish.setCompletedBy(userId);
        wishMapper.updateById(wish);
        return convertToVO(wish);
    }

    /**
     * 删除心愿（仅创建者）
     */
    public void deleteWish(Long wishId, Long userId, Long coupleId) {
        Wish wish = wishMapper.selectById(wishId);
        if (wish == null) {
            throw new BusinessException("心愿不存在");
        }
        if (!Objects.equals(wish.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该心愿");
        }
        if (!Objects.equals(wish.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己的心愿");
        }
        wishMapper.deleteById(wishId);
    }

    private WishVO convertToVO(Wish wish) {
        WishVO vo = new WishVO();
        vo.setId(wish.getId());
        vo.setCoupleId(wish.getCoupleId());
        vo.setUserId(wish.getUserId());
        vo.setTitle(wish.getTitle());
        vo.setDescription(wish.getDescription());
        vo.setIcon(wish.getIcon());
        vo.setCategory(wish.getCategory());
        vo.setPriority(wish.getPriority());
        vo.setStatus(wish.getStatus());
        vo.setCompletedTime(wish.getCompletedTime());
        vo.setCompletedBy(wish.getCompletedBy());
        vo.setCreateTime(wish.getCreateTime());
        vo.setUpdateTime(wish.getUpdateTime());

        User creator = userMapper.selectById(wish.getUserId());
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
            vo.setCreatorAvatar(creator.getAvatar());
        }
        if (wish.getCompletedBy() != null) {
            User completer = userMapper.selectById(wish.getCompletedBy());
            if (completer != null) {
                vo.setCompleterNickname(completer.getNickname());
            }
        }
        return vo;
    }
}
