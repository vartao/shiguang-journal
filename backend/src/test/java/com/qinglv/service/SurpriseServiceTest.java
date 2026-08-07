package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.SurpriseDTO;
import com.qinglv.dto.SurpriseVO;
import com.qinglv.entity.Surprise;
import com.qinglv.entity.User;
import com.qinglv.mapper.SurpriseMapper;
import com.qinglv.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 惊喜解锁 / 删除权限测试
 */
@ExtendWith(MockitoExtension.class)
class SurpriseServiceTest {

    @Mock
    private SurpriseMapper surpriseMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private SurpriseService surpriseService;

    private Surprise surprise;

    @BeforeEach
    void setUp() {
        surprise = new Surprise();
        surprise.setId(7L);
        surprise.setCoupleId(100L);
        surprise.setUserId(1L);
        surprise.setTitle("给你的信");
        surprise.setContent("我爱你");
        surprise.setHint("猜猜是谁");
        surprise.setUnlockCondition("click");
        surprise.setUnlocked(0);
    }

    @Test
    void unlockClick_shouldSucceed() {
        when(surpriseMapper.selectById(7L)).thenReturn(surprise);
        when(userMapper.selectById(1L)).thenReturn(null);

        SurpriseVO vo = surpriseService.unlockSurprise(7L, 2L, 100L, null);
        assertEquals(1, vo.getUnlocked());
        assertEquals("我爱你", vo.getContent());
        verify(surpriseMapper).updateById(any(Surprise.class));
    }

    @Test
    void unlockDate_beforeDue_shouldFail() {
        surprise.setUnlockCondition("date");
        surprise.setUnlockDate(LocalDate.now().plusDays(3));
        when(surpriseMapper.selectById(7L)).thenReturn(surprise);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> surpriseService.unlockSurprise(7L, 2L, 100L, null));
        assertTrue(ex.getMessage().contains("尚未到期"));
    }

    @Test
    void lockedDetail_shouldHideContent() {
        when(surpriseMapper.selectById(7L)).thenReturn(surprise);
        when(userMapper.selectById(1L)).thenReturn(null);

        SurpriseVO vo = surpriseService.getSurpriseDetail(7L, 100L);
        assertNull(vo.getContent());
        assertEquals("猜猜是谁", vo.getHint());
    }

    @Test
    void delete_onlyCreator() {
        when(surpriseMapper.selectById(7L)).thenReturn(surprise);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> surpriseService.deleteSurprise(7L, 2L, 100L));
        assertEquals(403, ex.getCode());
        verify(surpriseMapper, never()).deleteById(any());

        surpriseService.deleteSurprise(7L, 1L, 100L);
        verify(surpriseMapper).deleteById(7L);
    }

    @Test
    void create_passwordMissing_shouldFail() {
        SurpriseDTO dto = new SurpriseDTO();
        dto.setTitle("暗号");
        dto.setContent("内容");
        dto.setUnlockCondition("password");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> surpriseService.createSurprise(1L, 100L, dto));
        assertTrue(ex.getMessage().contains("解锁密码"));
    }
}
