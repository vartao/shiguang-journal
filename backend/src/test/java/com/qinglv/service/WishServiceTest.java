package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.WishDTO;
import com.qinglv.dto.WishVO;
import com.qinglv.entity.User;
import com.qinglv.entity.Wish;
import com.qinglv.mapper.UserMapper;
import com.qinglv.mapper.WishMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishServiceTest {

    @Mock
    private WishMapper wishMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private WishService wishService;

    private Wish wish(Long id, Long userId, Long completedBy, String title) {
        Wish wish = new Wish();
        wish.setId(id);
        wish.setCoupleId(100L);
        wish.setUserId(userId);
        wish.setCompletedBy(completedBy);
        wish.setTitle(title);
        wish.setStatus(0);
        return wish;
    }

    private User user(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setAvatar("/uploads/avatar_" + id + ".jpg");
        return user;
    }

    @Test
    void listShouldBatchLoadUsersWithoutPerRowQuery() {
        when(wishMapper.selectList(any())).thenReturn(List.of(
                wish(1L, 1L, null, "一起去看海"),
                wish(2L, 1L, 2L, "一起学滑雪")));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(1L, "小光"), user(2L, "小影")));

        List<WishVO> list = wishService.getWishList(100L, null);

        assertEquals(2, list.size());
        assertEquals("小光", list.get(0).getCreatorNickname());
        assertEquals("/uploads/avatar_1.jpg", list.get(0).getCreatorAvatar());
        assertEquals("小光", list.get(1).getCreatorNickname());
        assertEquals("小影", list.get(1).getCompleterNickname());
        // 列表只允许一次批量查询，禁止逐条 selectById
        verify(userMapper, times(1)).selectBatchIds(any());
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void listWithEmptyResultSkipsUserQuery() {
        when(wishMapper.selectList(any())).thenReturn(List.of());

        assertEquals(List.of(), wishService.getWishList(100L, 1));

        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void createShouldFillCreatorOnSinglePath() {
        Wish wish = wish(5L, 1L, null, "一起去看极光");
        when(wishMapper.insert(any(Wish.class))).thenAnswer(inv -> {
            wish.setId(5L);
            return 1;
        });
        when(userMapper.selectById(1L)).thenReturn(user(1L, "小光"));

        WishDTO dto = new WishDTO();
        dto.setTitle("一起去看极光");
        WishVO vo = wishService.createWish(1L, 100L, dto);

        assertEquals("一起去看极光", vo.getTitle());
        assertEquals("小光", vo.getCreatorNickname());
        verify(userMapper, never()).selectBatchIds(any());
    }

    @Test
    void listRequiresCoupleSpace() {
        BusinessException error = assertThrows(
                BusinessException.class, () -> wishService.getWishList(null, null));

        assertEquals("尚未加入情侣空间", error.getMessage());
    }
}
