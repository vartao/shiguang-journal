package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.MessageDTO;
import com.qinglv.dto.MessageVO;
import com.qinglv.entity.Message;
import com.qinglv.entity.User;
import com.qinglv.mapper.MessageMapper;
import com.qinglv.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    /**
     * 获取留言列表（按时间倒序取最近 N 条）
     */
    public List<MessageVO> list(Long coupleId, Integer limit) {
        checkCouple(coupleId);
        int size = normalizeLimit(limit);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getCoupleId, coupleId)
               .orderByDesc(Message::getCreateTime)
               .orderByDesc(Message::getId)
               .last("LIMIT " + size);

        List<Message> messages = messageMapper.selectList(wrapper);
        return convertToVOs(messages);
    }

    /**
     * 发布留言
     */
    public MessageVO create(Long userId, Long coupleId, MessageDTO dto) {
        checkCouple(coupleId);

        Message message = new Message();
        message.setCoupleId(coupleId);
        message.setUserId(userId);
        message.setContent(dto.getContent().trim());
        messageMapper.insert(message);

        return buildVO(message, userMapper.selectById(userId));
    }

    /**
     * 删除留言（仅本人）
     */
    public void delete(Long id, Long userId, Long coupleId) {
        checkCouple(coupleId);
        Message message = messageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException("留言不存在");
        }
        if (!Objects.equals(message.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该留言");
        }
        if (!Objects.equals(message.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己的留言");
        }
        messageMapper.deleteById(id);
    }

    private void checkCouple(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.min(Math.max(limit, 1), MAX_LIMIT);
    }

    private List<MessageVO> convertToVOs(List<Message> messages) {
        List<Long> userIds = messages.stream()
                .map(Message::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return messages.stream()
                .map(m -> buildVO(m, userMap.get(m.getUserId())))
                .collect(Collectors.toList());
    }

    private MessageVO buildVO(Message message, User user) {
        MessageVO vo = new MessageVO();
        vo.setId(message.getId());
        vo.setUserId(message.getUserId());
        vo.setContent(message.getContent());
        vo.setCreateTime(message.getCreateTime());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }
}
