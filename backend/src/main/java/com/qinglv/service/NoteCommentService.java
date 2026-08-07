package com.qinglv.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.NoteCommentDTO;
import com.qinglv.dto.NoteCommentVO;
import com.qinglv.entity.Note;
import com.qinglv.entity.NoteComment;
import com.qinglv.entity.User;
import com.qinglv.mapper.NoteCommentMapper;
import com.qinglv.mapper.NoteMapper;
import com.qinglv.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 笔记评论服务 - 按 couple_id 隔离，仅作者可删除自己的评论
 */
@Service
@RequiredArgsConstructor
public class NoteCommentService {

    private final NoteCommentMapper commentMapper;
    private final NoteMapper noteMapper;
    private final UserMapper userMapper;

    /**
     * 查询某篇笔记下的全部评论（按时间升序）
     */
    public List<NoteCommentVO> listByNote(Long noteId, Long userId, Long coupleId) {
        checkNoteAccess(noteId, coupleId);

        LambdaQueryWrapper<NoteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteComment::getNoteId, noteId)
               .orderByAsc(NoteComment::getCreateTime);
        List<NoteComment> comments = commentMapper.selectList(wrapper);
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        return convertToVOs(comments);
    }

    /**
     * 发表评论
     */
    @Transactional(rollbackFor = Exception.class)
    public NoteCommentVO addComment(Long noteId, Long userId, Long coupleId, NoteCommentDTO dto) {
        checkNoteAccess(noteId, coupleId);
        if (StrUtil.isBlank(dto.getContent())) {
            throw new BusinessException("评论内容不能为空");
        }
        String content = dto.getContent().trim();
        if (content.length() > 500) {
            throw new BusinessException("评论内容最多 500 字");
        }

        NoteComment comment = new NoteComment();
        comment.setNoteId(noteId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);

        return convertToVO(comment);
    }

    /**
     * 删除评论（仅评论作者）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId, Long coupleId) {
        NoteComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        checkNoteAccess(comment.getNoteId(), coupleId);
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己的评论");
        }
        commentMapper.deleteById(commentId);
    }

    private void checkNoteAccess(Long noteId, Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!Objects.equals(note.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权查看该笔记的评论");
        }
    }

    private List<NoteCommentVO> convertToVOs(List<NoteComment> comments) {
        List<Long> userIds = comments.stream()
                .map(NoteComment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream()
                .map(c -> buildVO(c, userMap.get(c.getUserId())))
                .collect(Collectors.toList());
    }

    private NoteCommentVO convertToVO(NoteComment comment) {
        User user = userMapper.selectById(comment.getUserId());
        return buildVO(comment, user);
    }

    private NoteCommentVO buildVO(NoteComment comment, User user) {
        NoteCommentVO vo = new NoteCommentVO();
        BeanUtils.copyProperties(comment, vo);
        if (user != null) {
            vo.setAuthorNickname(user.getNickname());
            vo.setAuthorAvatar(user.getAvatar());
        }
        return vo;
    }
}
