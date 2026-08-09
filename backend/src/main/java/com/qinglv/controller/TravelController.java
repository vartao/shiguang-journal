package com.qinglv.controller;

import com.qinglv.common.JwtInterceptor;
import com.qinglv.common.Result;
import com.qinglv.dto.TravelDTO;
import com.qinglv.dto.TravelMapSummaryVO;
import com.qinglv.entity.Travel;
import com.qinglv.entity.TravelPhoto;
import com.qinglv.entity.User;
import com.qinglv.mapper.UserMapper;
import com.qinglv.service.TravelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/travels")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;
    private final UserMapper userMapper;

    @GetMapping
    public Result<List<Travel>> getTravels(@RequestParam(required = false) Integer status) {
        Long coupleId = currentCoupleId();
        return Result.success(travelService.getTravels(coupleId, status));
    }

    @GetMapping("/map")
    public Result<List<TravelMapSummaryVO>> getMapSummary() {
        Long coupleId = currentCoupleId();
        return Result.success(travelService.getMapSummary(coupleId));
    }

    @PostMapping
    public Result<Travel> createTravel(@Valid @RequestBody TravelDTO dto) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();
        return Result.success("足迹已记录", travelService.createTravel(userId, coupleId, dto));
    }

    @GetMapping("/{id}")
    public Result<Travel> getTravelDetail(@PathVariable Long id) {
        Long coupleId = currentCoupleId();
        return Result.success(travelService.getTravelDetail(id, coupleId));
    }

    @PutMapping("/{id}")
    public Result<Travel> updateTravel(@PathVariable Long id, @Valid @RequestBody TravelDTO dto) {
        Long coupleId = currentCoupleId();
        return Result.success("更新成功", travelService.updateTravel(id, coupleId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTravel(@PathVariable Long id) {
        Long coupleId = currentCoupleId();
        travelService.deleteTravel(id, coupleId);
        return Result.success("删除成功", null);
    }

    @PostMapping("/{id}/photos")
    public Result<TravelPhoto> addPhoto(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = JwtInterceptor.getCurrentUserId();
        Long coupleId = currentCoupleId();
        return Result.success("照片已添加", travelService.addPhoto(userId, coupleId, id, body.get("photoUrl")));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public Result<Void> deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        Long coupleId = currentCoupleId();
        travelService.deletePhoto(photoId, coupleId);
        return Result.success("删除成功", null);
    }

    private Long currentCoupleId() {
        Long userId = JwtInterceptor.getCurrentUserId();
        User user = userMapper.selectById(userId);
        return user != null ? user.getCoupleId() : null;
    }
}
