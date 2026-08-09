package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.MessageDTO;
import com.qinglv.dto.MessageVO;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<List<MessageVO>> getMessages(@RequestParam(required = false) Integer limit) {
        Long coupleId = currentCoupleId();
        return Result.success(messageService.list(coupleId, limit));
    }

    @PostMapping
    public Result<MessageVO> createMessage(@Valid @RequestBody MessageDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();
        return Result.success("发送成功", messageService.create(userId, coupleId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();
        messageService.delete(id, userId, coupleId);
        return Result.success("删除成功", null);
    }

    private Long currentCoupleId() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return user != null ? user.getCoupleId() : null;
    }
}
