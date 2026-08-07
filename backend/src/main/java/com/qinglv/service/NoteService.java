package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qinglv.common.BusinessException;
import com.qinglv.common.PageResult;
import com.qinglv.dto.NoteDTO;
import com.qinglv.dto.NoteVO;
import com.qinglv.entity.Note;
import com.qinglv.entity.NoteImage;
import com.qinglv.entity.NoteLike;
import com.qinglv.entity.User;
import com.qinglv.mapper.NoteCommentMapper;
import com.qinglv.mapper.NoteImageMapper;
import com.qinglv.mapper.NoteLikeMapper;
import com.qinglv.mapper.NoteMapper;
import com.qinglv.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 笔记服务 - 笔记CRUD、时间线分页查询、点赞
 */
@Service
@RequiredArgsConstructor
public class NoteService {

    private final NoteMapper noteMapper;
    private final NoteImageMapper noteImageMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final UserMapper userMapper;
    private final NoteCommentMapper commentMapper;

    /**
     * 分页查询笔记时间线
     */
    public PageResult<NoteVO> getNoteTimeline(Long userId, Long coupleId, Integer pageNum, Integer pageSize, String mood) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Page<Note> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getCoupleId, coupleId);

        if (mood != null && !mood.isEmpty()) {
            wrapper.eq(Note::getMood, mood);
        }

        wrapper.orderByDesc(Note::getIsTop)
                .orderByDesc(Note::getNoteDate)
                .orderByDesc(Note::getCreateTime);

        Page<Note> notePage = noteMapper.selectPage(page, wrapper);
        List<Note> records = notePage.getRecords();

        Set<Long> likedNoteIds = loadLikedNoteIds(userId, records);

        List<NoteVO> voList = convertToVOs(records, likedNoteIds);

        return PageResult.of(
                notePage.getCurrent(),
                notePage.getSize(),
                notePage.getTotal(),
                notePage.getPages(),
                voList
        );
    }

    /**
     * 搜索笔记（标题+内容全文搜索）
     */
    public PageResult<NoteVO> searchNotes(Long userId, Long coupleId, String keyword, Integer pageNum, Integer pageSize) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            return getNoteTimeline(userId, coupleId, pageNum, pageSize, null);
        }

        Page<Note> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getCoupleId, coupleId)
               .and(w -> w.like(Note::getTitle, keyword.trim())
                          .or()
                          .like(Note::getContent, keyword.trim()))
               .orderByDesc(Note::getNoteDate)
               .orderByDesc(Note::getCreateTime);

        Page<Note> notePage = noteMapper.selectPage(page, wrapper);
        List<Note> records = notePage.getRecords();

        Set<Long> likedNoteIds = loadLikedNoteIds(userId, records);
        List<NoteVO> voList = convertToVOs(records, likedNoteIds);

        return PageResult.of(notePage.getCurrent(), notePage.getSize(), notePage.getTotal(), notePage.getPages(), voList);
    }

    /**
     * 获取笔记详情
     */
    public NoteVO getNoteDetail(Long noteId, Long userId, Long coupleId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!Objects.equals(note.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权查看该笔记");
        }
        boolean liked = isLiked(noteId, userId);
        return convertSingleNote(note, liked);
    }

    /**
     * 创建笔记
     */
    @Transactional(rollbackFor = Exception.class)
    public NoteVO createNote(Long userId, Long coupleId, NoteDTO dto) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }

        Note note = new Note();
        note.setCoupleId(coupleId);
        note.setUserId(userId);
        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        note.setCoverImage(dto.getCoverImage());
        note.setMood(dto.getMood());
        note.setWeather(dto.getWeather());
        note.setIsTop(dto.getIsTop() != null ? dto.getIsTop() : 0);
        note.setLikeCount(0);
        note.setNoteDate(dto.getNoteDate() != null ? dto.getNoteDate() : LocalDate.now());

        if ((note.getCoverImage() == null || note.getCoverImage().isEmpty())
                && dto.getImages() != null && !dto.getImages().isEmpty()) {
            note.setCoverImage(dto.getImages().get(0));
        }

        noteMapper.insert(note);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            saveNoteImages(note.getId(), dto.getImages());
        }

        return convertSingleNote(note, false);
    }

    /**
     * 更新笔记（仅作者）
     */
    @Transactional(rollbackFor = Exception.class)
    public NoteVO updateNote(Long noteId, Long userId, Long coupleId, NoteDTO dto) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!Objects.equals(note.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权编辑该笔记");
        }
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new BusinessException(403, "只能编辑自己的笔记");
        }

        note.setTitle(dto.getTitle());
        note.setContent(dto.getContent());
        if (dto.getCoverImage() != null) {
            note.setCoverImage(dto.getCoverImage());
        }
        if (dto.getMood() != null) {
            note.setMood(dto.getMood());
        }
        if (dto.getWeather() != null) {
            note.setWeather(dto.getWeather());
        }
        if (dto.getIsTop() != null) {
            note.setIsTop(dto.getIsTop());
        }
        if (dto.getNoteDate() != null) {
            note.setNoteDate(dto.getNoteDate());
        }

        noteMapper.updateById(note);

        if (dto.getImages() != null) {
            noteImageMapper.delete(
                    new LambdaQueryWrapper<NoteImage>().eq(NoteImage::getNoteId, noteId)
            );
            if (!dto.getImages().isEmpty()) {
                saveNoteImages(noteId, dto.getImages());
                if (note.getCoverImage() == null || note.getCoverImage().isEmpty()) {
                    note.setCoverImage(dto.getImages().get(0));
                    noteMapper.updateById(note);
                }
            }
        }

        boolean liked = isLiked(noteId, userId);
        return convertSingleNote(noteMapper.selectById(noteId), liked);
    }

    /**
     * 删除笔记（仅作者）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long noteId, Long userId, Long coupleId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new BusinessException("笔记不存在");
        }
        if (!Objects.equals(note.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该笔记");
        }
        if (!Objects.equals(note.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己的笔记");
        }

        noteMapper.deleteById(noteId);
        noteImageMapper.delete(
                new LambdaQueryWrapper<NoteImage>().eq(NoteImage::getNoteId, noteId)
        );
        noteLikeMapper.delete(
                new LambdaQueryWrapper<NoteLike>().eq(NoteLike::getNoteId, noteId)
        );
    }

    /**
     * 切换点赞：未赞则点赞，已赞则取消
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> likeNote(Long noteId, Long userId, Long coupleId) {
        Note note = noteMapper.selectById(noteId);
        if (note == null) throw new BusinessException("笔记不存在");
        if (!Objects.equals(note.getCoupleId(), coupleId)) throw new BusinessException(403, "无权操作该笔记");

        NoteLike existing = noteLikeMapper.selectOne(new LambdaQueryWrapper<NoteLike>()
                .eq(NoteLike::getNoteId, noteId).eq(NoteLike::getUserId, userId));
        boolean liked;
        if (existing == null) {
            NoteLike like = new NoteLike(); like.setNoteId(noteId); like.setUserId(userId);
            try {
                noteLikeMapper.insert(like);
                noteMapper.incrementLikeCount(noteId);
            } catch (DuplicateKeyException race) {
                // 唯一索引保证并发重复点赞幂等。
            }
            liked = true;
        } else {
            noteLikeMapper.deleteById(existing.getId());
            noteMapper.decrementLikeCount(noteId);
            liked = false;
        }

        Note refreshed = noteMapper.selectById(noteId);
        int current = refreshed == null || refreshed.getLikeCount() == null ? 0 : Math.max(0, refreshed.getLikeCount());
        Map<String, Object> result = new HashMap<>();
        result.put("id", noteId); result.put("likeCount", current); result.put("likes", current); result.put("liked", liked);
        return result;
    }

    private Set<Long> loadLikedNoteIds(Long userId, List<Note> notes) {
        if (userId == null || notes == null || notes.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> noteIds = notes.stream().map(Note::getId).collect(Collectors.toList());
        List<NoteLike> likes = noteLikeMapper.selectList(
                new LambdaQueryWrapper<NoteLike>()
                        .eq(NoteLike::getUserId, userId)
                        .in(NoteLike::getNoteId, noteIds)
        );
        Set<Long> set = new HashSet<>();
        for (NoteLike like : likes) {
            set.add(like.getNoteId());
        }
        return set;
    }

    private boolean isLiked(Long noteId, Long userId) {
        if (userId == null || noteId == null) {
            return false;
        }
        Long count = noteLikeMapper.selectCount(
                new LambdaQueryWrapper<NoteLike>()
                        .eq(NoteLike::getNoteId, noteId)
                        .eq(NoteLike::getUserId, userId)
        );
        return count != null && count > 0;
    }

    private void saveNoteImages(Long noteId, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            NoteImage image = new NoteImage();
            image.setNoteId(noteId);
            image.setImageUrl(imageUrls.get(i));
            image.setSortOrder(i + 1);
            noteImageMapper.insert(image);
        }
    }

    private NoteVO convertSingleNote(Note note, boolean liked) {
        Set<Long> likedNoteIds = liked && note.getId() != null
                ? Collections.singleton(note.getId())
                : Collections.emptySet();
        return convertToVOs(Collections.singletonList(note), likedNoteIds).get(0);
    }

    /**
     * 批量装配时间线展示数据，避免每篇笔记分别查询作者和图片造成 N+1。
     */
    private List<NoteVO> convertToVOs(List<Note> notes, Set<Long> likedNoteIds) {
        if (notes == null || notes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = notes.stream()
                .map(Note::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> authorsById = new HashMap<>();
        if (!authorIds.isEmpty()) {
            for (User author : userMapper.selectBatchIds(authorIds)) {
                authorsById.put(author.getId(), author);
            }
        }

        List<Long> noteIds = notes.stream()
                .map(Note::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<String>> imageUrlsByNoteId = new HashMap<>();
        if (!noteIds.isEmpty()) {
            List<NoteImage> images = noteImageMapper.selectList(
                    new LambdaQueryWrapper<NoteImage>()
                            .in(NoteImage::getNoteId, noteIds)
                            .orderByAsc(NoteImage::getNoteId)
                            .orderByAsc(NoteImage::getSortOrder)
            );
            for (NoteImage image : images) {
                imageUrlsByNoteId
                        .computeIfAbsent(image.getNoteId(), ignored -> new ArrayList<>())
                        .add(image.getImageUrl());
            }
        }

        Map<Long, Integer> commentCountMap = new HashMap<>();
        if (!noteIds.isEmpty()) {
            for (Map<String, Object> row : commentMapper.selectCountByNoteIds(noteIds)) {
                Long noteId = ((Number) row.get("noteId")).longValue();
                Integer cnt = ((Number) row.get("cnt")).intValue();
                commentCountMap.put(noteId, cnt);
            }
        }

        return notes.stream()
                .map(note -> buildNoteVO(
                        note,
                        likedNoteIds.contains(note.getId()),
                        authorsById.get(note.getUserId()),
                        imageUrlsByNoteId.getOrDefault(note.getId(), Collections.emptyList()),
                        commentCountMap.getOrDefault(note.getId(), 0)))
                .collect(Collectors.toList());
    }

    private NoteVO buildNoteVO(Note note, boolean liked, User author, List<String> imageUrls, Integer commentCount) {
        NoteVO vo = new NoteVO();
        vo.setId(note.getId());
        vo.setCoupleId(note.getCoupleId());
        vo.setUserId(note.getUserId());
        vo.setTitle(note.getTitle());
        vo.setContent(note.getContent());
        vo.setCoverImage(note.getCoverImage());
        vo.setMood(note.getMood());
        vo.setWeather(note.getWeather());
        vo.setIsTop(note.getIsTop());
        vo.setLikeCount(note.getLikeCount());
        vo.setLikes(note.getLikeCount());
        vo.setLiked(liked);
        vo.setCommentCount(commentCount);
        vo.setCreateTime(note.getCreateTime());
        vo.setUpdateTime(note.getUpdateTime());
        vo.setCreatedAt(note.getCreateTime());
        vo.setNoteDate(note.getNoteDate() != null
                ? note.getNoteDate()
                : note.getCreateTime() != null ? note.getCreateTime().toLocalDate() : null);

        if (author != null) {
            vo.setAuthorNickname(author.getNickname());
            vo.setAuthorName(author.getNickname());
            vo.setAuthorAvatar(author.getAvatar());
        }

        vo.setImages(imageUrls);

        return vo;
    }
}
