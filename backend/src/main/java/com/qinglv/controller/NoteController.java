package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.PageResult;
import com.qinglv.common.Result;
import com.qinglv.dto.NoteCommentDTO;
import com.qinglv.dto.NoteCommentVO;
import com.qinglv.dto.NoteDTO;
import com.qinglv.dto.NoteVO;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.NoteCommentService;
import com.qinglv.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 笔记控制器
 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteCommentService noteCommentService;
    private final UserMapper userMapper;

    /**
     * 分页查询笔记时间线
     * GET /api/notes?pageNum=1&pageSize=10&mood=happy
     * 同时兼容 page / size
     */
    @GetMapping
    public Result<PageResult<NoteVO>> getNotes(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String mood) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        int pn = firstNonNull(pageNum, page, 1);
        int ps = firstNonNull(pageSize, size, 10);

        PageResult<NoteVO> pageResult = noteService.getNoteTimeline(userId, coupleId, pn, ps, mood);
        return Result.success(pageResult);
    }

    /**
     * 搜索笔记
     * GET /api/notes/search?keyword=xxx&page=1&size=10
     */
    @GetMapping("/search")
    public Result<PageResult<NoteVO>> searchNotes(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        PageResult<NoteVO> pageResult = noteService.searchNotes(userId, coupleId, keyword, page, size);
        return Result.success(pageResult);
    }

    /**
     * 创建笔记
     * POST /api/notes
     */
    @PostMapping
    public Result<NoteVO> createNote(@Valid @RequestBody NoteDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        NoteVO noteVO = noteService.createNote(userId, coupleId, dto);
        return Result.success("发布成功", noteVO);
    }

    /**
     * 获取笔记详情
     * GET /api/notes/{id}
     */
    @GetMapping("/{id}")
    public Result<NoteVO> getNoteDetail(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        NoteVO noteVO = noteService.getNoteDetail(id, userId, coupleId);
        return Result.success(noteVO);
    }

    /**
     * 更新笔记
     * PUT /api/notes/{id}
     */
    @PutMapping("/{id}")
    public Result<NoteVO> updateNote(@PathVariable Long id, @Valid @RequestBody NoteDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        NoteVO noteVO = noteService.updateNote(id, userId, coupleId, dto);
        return Result.success("更新成功", noteVO);
    }

    /**
     * 删除笔记
     * DELETE /api/notes/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        noteService.deleteNote(id, userId, coupleId);
        return Result.success("删除成功", null);
    }

    /**
     * 点赞笔记
     * POST /api/notes/{id}/like
     */
    @PostMapping("/{id}/like")
    public Result<Map<String, Object>> likeNote(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        Map<String, Object> result = noteService.likeNote(id, userId, coupleId);
        String message = Boolean.TRUE.equals(result.get("liked")) ? "点赞成功" : "已取消点赞";
        return Result.success(message, result);
    }

    /**
     * 查询笔记评论列表
     * GET /api/notes/{id}/comments
     */
    @GetMapping("/{id}/comments")
    public Result<List<NoteCommentVO>> listComments(@PathVariable Long id) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        List<NoteCommentVO> comments = noteCommentService.listByNote(id, userId, coupleId);
        return Result.success(comments);
    }

    /**
     * 发表评论
     * POST /api/notes/{id}/comments
     */
    @PostMapping("/{id}/comments")
    public Result<NoteCommentVO> addComment(@PathVariable Long id, @Valid @RequestBody NoteCommentDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        NoteCommentVO commentVO = noteCommentService.addComment(id, userId, coupleId, dto);
        return Result.success("评论成功", commentVO);
    }

    /**
     * 删除评论（仅评论作者）
     * DELETE /api/notes/comments/{commentId}
     */
    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        Long coupleId = user != null ? user.getCoupleId() : null;

        noteCommentService.deleteComment(commentId, userId, coupleId);
        return Result.success("删除成功", null);
    }

    private int firstNonNull(Integer a, Integer b, int defaultValue) {
        if (a != null) {
            return a;
        }
        if (b != null) {
            return b;
        }
        return defaultValue;
    }
}
