package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.WishDTO;
import com.qinglv.dto.WishVO;
import com.qinglv.service.WishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishes")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;
    private final CurrentUserContext currentUser;

    @GetMapping
    public Result<List<WishVO>> getWishes(@RequestParam(required = false) Integer status) {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(wishService.getWishList(coupleId, status));
    }

    @PostMapping
    public Result<WishVO> createWish(@Valid @RequestBody WishDTO dto) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("许愿成功", wishService.createWish(userId, coupleId, dto));
    }

    @PutMapping("/{id}")
    public Result<WishVO> updateWish(@PathVariable Long id, @Valid @RequestBody WishDTO dto) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("更新成功", wishService.updateWish(id, userId, coupleId, dto));
    }

    @PostMapping("/{id}/complete")
    public Result<WishVO> completeWish(@PathVariable Long id) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("心愿已达成", wishService.completeWish(id, userId, coupleId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteWish(@PathVariable Long id) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        wishService.deleteWish(id, userId, coupleId);
        return Result.success("删除成功", null);
    }
}
