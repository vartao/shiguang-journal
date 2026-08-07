package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.common.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 笔记服务核心逻辑测试（创建 / 权限 / 点赞）
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteMapper noteMapper;
    @Mock
    private NoteImageMapper noteImageMapper;
    @Mock
    private NoteLikeMapper noteLikeMapper;
    @Mock
    private NoteCommentMapper commentMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private NoteService noteService;

    private Note existingNote;

    @BeforeEach
    void setUp() {
        existingNote = new Note();
        existingNote.setId(10L);
        existingNote.setCoupleId(100L);
        existingNote.setUserId(1L);
        existingNote.setTitle("旧标题");
        existingNote.setContent("旧内容");
        existingNote.setLikeCount(0);
        existingNote.setIsTop(0);
    }

    @Test
    void createNote_withoutCouple_shouldFail() {
        NoteDTO dto = new NoteDTO();
        dto.setTitle("标题");
        dto.setContent("内容");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> noteService.createNote(1L, null, dto));
        assertTrue(ex.getMessage().contains("情侣空间"));
    }

    @Test
    void createNote_shouldPersistWeatherAndTop() {
        NoteDTO dto = new NoteDTO();
        dto.setTitle("雨天日记");
        dto.setContent("下了好大的雨");
        dto.setMood("calm");
        dto.setWeather("rainy");
        dto.setIsTop(1);
        dto.setNoteDate(LocalDate.of(2024, 5, 20));

        when(noteMapper.insert(any(Note.class))).thenAnswer(invocation -> {
            Note n = invocation.getArgument(0);
            n.setId(99L);
            return 1;
        });
        when(noteImageMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(commentMapper.selectCountByNoteIds(any())).thenReturn(Collections.emptyList());

        NoteVO vo = noteService.createNote(1L, 100L, dto);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteMapper).insert(captor.capture());
        Note saved = captor.getValue();
        assertEquals("rainy", saved.getWeather());
        assertEquals(1, saved.getIsTop());
        assertEquals(LocalDate.of(2024, 5, 20), saved.getNoteDate());
        assertEquals(100L, saved.getCoupleId());
        assertEquals(99L, vo.getId());
    }

    @Test
    void timeline_shouldBatchLoadAuthorsAndImages() {
        Note second = new Note();
        second.setId(11L);
        second.setCoupleId(100L);
        second.setUserId(2L);
        second.setTitle("第二篇");
        second.setLikeCount(0);

        Page<Note> page = new Page<>(1, 10);
        page.setRecords(List.of(existingNote, second));
        page.setTotal(2);
        when(noteMapper.selectPage(any(Page.class), any())).thenReturn(page);
        when(noteLikeMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(commentMapper.selectCountByNoteIds(any())).thenReturn(Collections.emptyList());

        User firstAuthor = new User();
        firstAuthor.setId(1L);
        firstAuthor.setNickname("小青");
        User secondAuthor = new User();
        secondAuthor.setId(2L);
        secondAuthor.setNickname("小侣");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(firstAuthor, secondAuthor));

        NoteImage firstImage = new NoteImage();
        firstImage.setNoteId(10L);
        firstImage.setImageUrl("/uploads/first.jpg");
        NoteImage secondImage = new NoteImage();
        secondImage.setNoteId(11L);
        secondImage.setImageUrl("/uploads/second.jpg");
        when(noteImageMapper.selectList(any())).thenReturn(List.of(firstImage, secondImage));

        PageResult<NoteVO> result = noteService.getNoteTimeline(1L, 100L, 1, 10, null);

        assertEquals(2, result.getRecords().size());
        assertEquals("小青", result.getRecords().get(0).getAuthorNickname());
        assertEquals(List.of("/uploads/second.jpg"), result.getRecords().get(1).getImages());
        verify(userMapper, times(1)).selectBatchIds(any());
        verify(userMapper, never()).selectById(any());
        verify(noteImageMapper, times(1)).selectList(any());
    }

    @Test
    void updateNote_notAuthor_shouldForbid() {
        when(noteMapper.selectById(10L)).thenReturn(existingNote);

        NoteDTO dto = new NoteDTO();
        dto.setTitle("改");
        dto.setContent("改");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> noteService.updateNote(10L, 2L, 100L, dto));
        assertEquals(403, ex.getCode());
    }

    @Test
    void likeNote_shouldToggleWithAtomicCounters() {
        Note likedNote = new Note();
        likedNote.setId(10L); likedNote.setCoupleId(100L); likedNote.setLikeCount(1);
        when(noteMapper.selectById(10L)).thenReturn(existingNote, likedNote);
        when(noteLikeMapper.selectOne(any())).thenReturn(null);
        Map<String, Object> liked = noteService.likeNote(10L, 2L, 100L);
        assertEquals(true, liked.get("liked"));
        assertEquals(1, liked.get("likeCount"));
        verify(noteLikeMapper).insert(any(NoteLike.class));
        verify(noteMapper).incrementLikeCount(10L);

        reset(noteMapper, noteLikeMapper);
        NoteLike existing = new NoteLike(); existing.setId(5L); existing.setNoteId(10L); existing.setUserId(2L);
        Note beforeUnlike = new Note(); beforeUnlike.setId(10L); beforeUnlike.setCoupleId(100L); beforeUnlike.setLikeCount(1);
        Note unlikedNote = new Note(); unlikedNote.setId(10L); unlikedNote.setCoupleId(100L); unlikedNote.setLikeCount(0);
        when(noteMapper.selectById(10L)).thenReturn(beforeUnlike, unlikedNote);
        when(noteLikeMapper.selectOne(any())).thenReturn(existing);
        Map<String, Object> unliked = noteService.likeNote(10L, 2L, 100L);
        assertEquals(false, unliked.get("liked"));
        assertEquals(0, unliked.get("likeCount"));
        verify(noteLikeMapper).deleteById(5L);
        verify(noteMapper).decrementLikeCount(10L);
    }

    @Test
    void deleteNote_otherCouple_shouldForbid() {
        when(noteMapper.selectById(10L)).thenReturn(existingNote);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> noteService.deleteNote(10L, 1L, 999L));
        assertEquals(403, ex.getCode());
        verify(noteMapper, never()).deleteById(any());
    }
}
