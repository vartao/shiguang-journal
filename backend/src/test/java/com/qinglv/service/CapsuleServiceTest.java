package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.CapsuleDTO;
import com.qinglv.dto.CapsuleVO;
import com.qinglv.entity.Capsule;
import com.qinglv.mapper.CapsuleMapper;
import com.qinglv.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapsuleServiceTest {

    @Mock
    private CapsuleMapper capsuleMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CapsuleService capsuleService;

    private Capsule buildCapsule(LocalDate openDate, int isOpened) {
        Capsule capsule = new Capsule();
        capsule.setId(10L);
        capsule.setCoupleId(100L);
        capsule.setUserId(1L);
        capsule.setTitle("给未来的我们");
        capsule.setMessage("秘密信件内容");
        capsule.setPhotoUrls("[\"/uploads/a.jpg\"]");
        capsule.setOpenDate(openDate);
        capsule.setIsOpened(isOpened);
        return capsule;
    }

    @Test
    void listMasksMessageAndPhotosBeforeOpenDate() {
        Capsule capsule = buildCapsule(LocalDate.now().plusDays(5), 0);
        when(capsuleMapper.selectList(any())).thenReturn(List.of(capsule));

        List<CapsuleVO> list = capsuleService.getCapsuleList(100L);

        CapsuleVO vo = list.get(0);
        assertNull(vo.getMessage());
        assertNull(vo.getPhotoUrls());
        assertFalse(vo.getCanOpen());
        assertEquals(5L, vo.getDaysLeft());
        assertEquals("给未来的我们", vo.getTitle());
    }

    @Test
    void listReturnsFullContentAfterOpened() {
        Capsule capsule = buildCapsule(LocalDate.now().plusDays(5), 1);
        when(capsuleMapper.selectList(any())).thenReturn(List.of(capsule));

        List<CapsuleVO> list = capsuleService.getCapsuleList(100L);

        CapsuleVO vo = list.get(0);
        assertEquals("秘密信件内容", vo.getMessage());
        assertEquals(List.of("/uploads/a.jpg"), vo.getPhotoUrls());
        assertFalse(vo.getCanOpen());
        assertEquals(0L, vo.getDaysLeft());
    }

    @Test
    void listReturnsFullContentWhenDueButNotOpened() {
        Capsule capsule = buildCapsule(LocalDate.now(), 0);
        when(capsuleMapper.selectList(any())).thenReturn(List.of(capsule));

        List<CapsuleVO> list = capsuleService.getCapsuleList(100L);

        CapsuleVO vo = list.get(0);
        assertEquals("秘密信件内容", vo.getMessage());
        assertTrue(vo.getCanOpen());
        assertEquals(0L, vo.getDaysLeft());
    }

    @Test
    void listRequiresCoupleSpace() {
        BusinessException error = assertThrows(
                BusinessException.class, () -> capsuleService.getCapsuleList(null));

        assertEquals("尚未加入情侣空间", error.getMessage());
    }

    @Test
    void createRejectsOpenDateNotInFuture() {
        CapsuleDTO dto = new CapsuleDTO();
        dto.setTitle("标题");
        dto.setOpenDate(LocalDate.now());

        BusinessException error = assertThrows(
                BusinessException.class, () -> capsuleService.createCapsule(1L, 100L, dto));

        assertEquals("开启日期要是未来的某一天", error.getMessage());
        verify(capsuleMapper, never()).insert(any(Capsule.class));
    }

    @Test
    void createSerializesPhotosAndDefaultsToLocked() {
        CapsuleDTO dto = new CapsuleDTO();
        dto.setTitle("标题");
        dto.setMessage("内容");
        dto.setPhotoUrls(List.of("/uploads/a.jpg", "/uploads/b.jpg"));
        dto.setOpenDate(LocalDate.now().plusDays(10));

        CapsuleVO vo = capsuleService.createCapsule(1L, 100L, dto);

        verify(capsuleMapper).insert(any(Capsule.class));
        assertEquals(0, vo.getIsOpened());
        assertFalse(vo.getCanOpen());
        assertEquals(10L, vo.getDaysLeft());
        // 新建后未到期，内容保持隐藏
        assertNull(vo.getMessage());
        assertNull(vo.getPhotoUrls());
    }

    @Test
    void openBeforeOpenDateIsRejected() {
        Capsule capsule = buildCapsule(LocalDate.now().plusDays(3), 0);
        when(capsuleMapper.selectById(10L)).thenReturn(capsule);

        BusinessException error = assertThrows(
                BusinessException.class, () -> capsuleService.openCapsule(10L, 1L, 100L));

        assertEquals("还没到开启的日子，再等等", error.getMessage());
        verify(capsuleMapper, never()).updateById(any(Capsule.class));
    }

    @Test
    void openDueCapsuleSucceedsAndIsIdempotent() {
        Capsule capsule = buildCapsule(LocalDate.now(), 0);
        when(capsuleMapper.selectById(10L)).thenReturn(capsule);

        CapsuleVO first = capsuleService.openCapsule(10L, 2L, 100L);

        assertEquals(1, first.getIsOpened());
        assertEquals(2L, first.getOpenedBy());
        assertEquals("秘密信件内容", first.getMessage());

        // 再次拆开直接返回，不再写库
        CapsuleVO second = capsuleService.openCapsule(10L, 1L, 100L);
        assertEquals(1, second.getIsOpened());
        verify(capsuleMapper, times(1)).updateById(any(Capsule.class));
    }

    @Test
    void openCapsuleFromAnotherCoupleIsRejected() {
        Capsule capsule = buildCapsule(LocalDate.now(), 0);
        capsule.setCoupleId(999L);
        when(capsuleMapper.selectById(10L)).thenReturn(capsule);

        BusinessException error = assertThrows(
                BusinessException.class, () -> capsuleService.openCapsule(10L, 1L, 100L));

        assertEquals(403, error.getCode());
        verify(capsuleMapper, never()).updateById(any(Capsule.class));
    }

    @Test
    void deleteByNonCreatorIsRejected() {
        Capsule capsule = buildCapsule(LocalDate.now().plusDays(3), 0);
        when(capsuleMapper.selectById(10L)).thenReturn(capsule);

        BusinessException error = assertThrows(
                BusinessException.class, () -> capsuleService.deleteCapsule(10L, 2L, 100L));

        assertEquals(403, error.getCode());
        verify(capsuleMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void deleteByCreatorSucceeds() {
        Capsule capsule = buildCapsule(LocalDate.now().plusDays(3), 0);
        when(capsuleMapper.selectById(10L)).thenReturn(capsule);

        capsuleService.deleteCapsule(10L, 1L, 100L);

        verify(capsuleMapper).deleteById(10L);
    }
}
