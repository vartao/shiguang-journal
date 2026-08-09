package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.MessageDTO;
import com.qinglv.dto.MessageVO;
import com.qinglv.entity.Message;
import com.qinglv.entity.User;
import com.qinglv.mapper.MessageMapper;
import com.qinglv.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageMapper messageMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MessageService messageService;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setNickname("小爱");
        alice.setAvatar("/uploads/alice.jpg");

        bob = new User();
        bob.setId(2L);
        bob.setNickname("小博");
        bob.setAvatar("/uploads/bob.jpg");
    }

    @Test
    void createPersistsMessageAndReturnsAuthorInfo() {
        MessageDTO dto = new MessageDTO();
        dto.setContent("今天也想你");
        when(userMapper.selectById(1L)).thenReturn(alice);

        MessageVO vo = messageService.create(1L, 100L, dto);

        assertEquals("今天也想你", vo.getContent());
        assertEquals("小爱", vo.getNickname());
        assertEquals("/uploads/alice.jpg", vo.getAvatar());
        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    void listFillsNicknamesWithBatchUserLookup() {
        Message m1 = new Message();
        m1.setId(11L);
        m1.setUserId(1L);
        m1.setContent("早安");
        m1.setCreateTime(LocalDateTime.of(2026, 8, 8, 9, 0));

        Message m2 = new Message();
        m2.setId(12L);
        m2.setUserId(2L);
        m2.setContent("晚安");
        m2.setCreateTime(LocalDateTime.of(2026, 8, 8, 22, 0));

        when(messageMapper.selectList(any())).thenReturn(List.of(m2, m1));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(alice, bob));

        List<MessageVO> list = messageService.list(100L, null);

        assertEquals(2, list.size());
        assertEquals("小博", list.get(0).getNickname());
        assertEquals("小爱", list.get(1).getNickname());
        assertEquals("/uploads/alice.jpg", list.get(1).getAvatar());
    }

    @Test
    void listRejectsUserWithoutCouple() {
        BusinessException error = assertThrows(
                BusinessException.class, () -> messageService.list(null, null));

        verify(messageMapper, never()).selectList(any());
        assertEquals("尚未加入情侣空间", error.getMessage());
    }

    @Test
    void deleteByNonAuthorIsRejectedWith403() {
        Message message = new Message();
        message.setId(7L);
        message.setCoupleId(100L);
        message.setUserId(2L);
        when(messageMapper.selectById(7L)).thenReturn(message);

        BusinessException error = assertThrows(
                BusinessException.class, () -> messageService.delete(7L, 1L, 100L));

        assertEquals(403, error.getCode());
        verify(messageMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteByAuthorSucceeds() {
        Message message = new Message();
        message.setId(7L);
        message.setCoupleId(100L);
        message.setUserId(1L);
        when(messageMapper.selectById(7L)).thenReturn(message);

        messageService.delete(7L, 1L, 100L);

        verify(messageMapper).deleteById(7L);
    }
}
