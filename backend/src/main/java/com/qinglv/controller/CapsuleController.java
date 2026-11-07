package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.CapsuleDTO;
import com.qinglv.dto.CapsuleVO;
import com.qinglv.service.CapsuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/capsules")
@RequiredArgsConstructor
public class CapsuleController {

    private final CapsuleService capsuleService;
    private final CurrentUserContext currentUser;

    @GetMapping
    public Result<List<CapsuleVO>> getCapsules() {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(capsuleService.getCapsuleList(coupleId));
    }

    @PostMapping
    public Result<CapsuleVO> createCapsule(@Valid @RequestBody CapsuleDTO dto) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("封存成功", capsuleService.createCapsule(userId, coupleId, dto));
    }

    @PostMapping("/{id}/open")
    public Result<CapsuleVO> openCapsule(@PathVariable Long id) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("胶囊已开启", capsuleService.openCapsule(id, userId, coupleId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteCapsule(@PathVariable Long id) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        capsuleService.deleteCapsule(id, userId, coupleId);
        return Result.success("删除成功", null);
    }
}
