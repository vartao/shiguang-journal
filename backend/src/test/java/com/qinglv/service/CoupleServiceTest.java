package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.CoupleVO;
import com.qinglv.entity.Couple;
import com.qinglv.entity.User;
import com.qinglv.mapper.CoupleMapper;
import com.qinglv.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 情侣空间创建 / 绑定引导文案测试
 */
@ExtendWith(MockitoExtension.class)
class CoupleServiceTest {

    @Mock
    private CoupleMapper coupleMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CoupleService coupleService;

    @Test
    void createCouple_whenAlreadyJoined_shouldFail() {
        User user = new User();
        user.setId(1L);
        user.setCoupleId(9L);
        when(userMapper.selectById(1L)).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> coupleService.createCouple(1L, Map.of("name", "小窝")));
        assertTrue(ex.getMessage().contains("情侣空间"));
    }

    @Test
    void createCouple_shouldGenerateInviteCode() {
        User user = new User();
        user.setId(1L);
        user.setNickname("小明");
        user.setCoupleId(null);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(coupleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(coupleMapper.insert(any(Couple.class))).thenAnswer(invocation -> {
            Couple c = invocation.getArgument(0);
            c.setId(88L);
            return 1;
        });

        Map<String, String> body = new HashMap<>();
        body.put("name", "我们的小窝");
        body.put("togetherDate", "2024-05-20");

        CoupleVO vo = coupleService.createCouple(1L, body);
        assertEquals(88L, vo.getId());
        assertEquals("我们的小窝", vo.getName());
        assertNotNull(vo.getInviteCode());
        assertEquals(6, vo.getInviteCode().length());
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void bindPartner_invalidCode_shouldFriendlyMessage() {
        User user = new User();
        user.setId(2L);
        user.setCoupleId(null);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(coupleMapper.selectByInviteCodeForUpdate("ABCDEF")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> coupleService.bindPartner(2L, "ABCDEF"));
        assertTrue(ex.getMessage().contains("邀请码不正确"));
    }

    @Test
    void bindPartner_fullCouple_shouldFriendlyMessage() {
        User user = new User();
        user.setId(3L);
        user.setCoupleId(null);
        when(userMapper.selectById(3L)).thenReturn(user);

        Couple couple = new Couple();
        couple.setId(1L);
        couple.setUserId1(1L);
        couple.setUserId2(2L);
        couple.setInviteCode("ABCDEF");
        when(coupleMapper.selectByInviteCodeForUpdate("ABCDEF")).thenReturn(couple);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> coupleService.bindPartner(3L, "abcdef"));
        assertTrue(ex.getMessage().contains("住满"));
    }

    @Test
    void bindPartner_success_shouldSetMember() {
        User user = new User();
        user.setId(2L);
        user.setCoupleId(null);
        when(userMapper.selectById(2L)).thenReturn(user);

        Couple couple = new Couple();
        couple.setId(10L);
        couple.setUserId1(1L);
        couple.setUserId2(null);
        couple.setInviteCode("LOVEOK");
        couple.setName("小窝");
        when(coupleMapper.selectByInviteCodeForUpdate("LOVEOK")).thenReturn(couple);

        User u1 = new User();
        u1.setId(1L);
        u1.setNickname("TA");
        when(userMapper.selectById(1L)).thenReturn(u1);

        CoupleVO vo = coupleService.bindPartner(2L, "loveok");
        assertEquals(10L, vo.getId());
        assertEquals(2L, couple.getUserId2());
        verify(userMapper).updateById(argThat(u -> "member".equals(u.getRole()) && Long.valueOf(10L).equals(u.getCoupleId())));
    }
}
