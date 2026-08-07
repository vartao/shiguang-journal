package com.qinglv.integration;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.LoginVO;
import com.qinglv.dto.NoteDTO;
import com.qinglv.dto.RegisterDTO;
import com.qinglv.service.AuthService;
import com.qinglv.service.CoupleService;
import com.qinglv.service.NoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 情侣空间隔离与笔记越权访问的集成测试：使用真实 MySQL 验证 couple_id 隔离。
 */
class CoupleIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private CoupleService coupleService;
    @Autowired
    private NoteService noteService;

    @Test
    @DisplayName("用户绑定空间后可以创建并查看笔记，其他用户越权访问返回 403")
    void shouldIsolateNotesBetweenCouples() {
        // Given：用户 A 创建小窝
        LoginVO userA = register("alice", "Alice");
        Long coupleId = userA.getUser().getCoupleId();
        assertNotNull(coupleId, "注册成功后应在小窝中");
        String inviteCode = coupleService.getCoupleInfo(userA.getUserId()).getInviteCode();

        // Given：用户 B 通过邀请码加入 A 的小窝
        LoginVO userB = registerWithInvite("bob", "Bob", inviteCode);
        assertEquals(coupleId, userB.getUser().getCoupleId(), "B 应与 A 在同一空间");

        // When：A 创建笔记
        NoteDTO dto = new NoteDTO();
        dto.setTitle("今天天气很好");
        dto.setContent("一起去公园散步了");
        dto.setMood("happy");
        dto.setWeather("sunny");
        dto.setNoteDate(LocalDate.now());
        var created = noteService.createNote(userA.getUserId(), coupleId, dto);
        assertNotNull(created.getId(), "笔记应保存成功");

        // Then：B 可以查看同一篇笔记
        var detailForB = noteService.getNoteDetail(created.getId(), userB.getUserId(), coupleId);
        assertEquals("今天天气很好", detailForB.getTitle());

        // Given：用户 C 在另一个独立空间
        LoginVO userC = register("carol", "Carol");
        Long carolCoupleId = coupleService.createCouple(userC.getUserId(), null).getId();

        // Then & Expect：C 无权访问 A 的笔记
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> noteService.getNoteDetail(created.getId(), userC.getUserId(), carolCoupleId)
        );
        assertEquals(403, exception.getCode(), "跨空间访问应返回 403");
    }

    private LoginVO register(String username, String nickname) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword("123456");
        dto.setNickname(nickname);
        dto.setGender(0);
        return authService.register(dto);
    }

    private LoginVO registerWithInvite(String username, String nickname, String inviteCode) {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername(username);
        dto.setPassword("123456");
        dto.setNickname(nickname);
        dto.setGender(0);
        dto.setInviteCode(inviteCode);
        return authService.register(dto);
    }
}
