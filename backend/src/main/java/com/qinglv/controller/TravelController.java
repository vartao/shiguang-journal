package com.qinglv.controller;

import com.qinglv.common.CurrentUserContext;
import com.qinglv.common.Result;
import com.qinglv.dto.TravelDTO;
import com.qinglv.dto.TravelMapSummaryVO;
import com.qinglv.entity.Travel;
import com.qinglv.entity.TravelPhoto;
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
    private final CurrentUserContext currentUser;

    @GetMapping
    public Result<List<Travel>> getTravels(@RequestParam(required = false) Integer status) {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(travelService.getTravels(coupleId, status));
    }

    @GetMapping("/map")
    public Result<List<TravelMapSummaryVO>> getMapSummary() {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(travelService.getMapSummary(coupleId));
    }

    @PostMapping
    public Result<Travel> createTravel(@Valid @RequestBody TravelDTO dto) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("足迹已记录", travelService.createTravel(userId, coupleId, dto));
    }

    @GetMapping("/{id}")
    public Result<Travel> getTravelDetail(@PathVariable Long id) {
        Long coupleId = currentUser.getCoupleId();
        return Result.success(travelService.getTravelDetail(id, coupleId));
    }

    @PutMapping("/{id}")
    public Result<Travel> updateTravel(@PathVariable Long id, @Valid @RequestBody TravelDTO dto) {
        Long coupleId = currentUser.getCoupleId();
        return Result.success("更新成功", travelService.updateTravel(id, coupleId, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTravel(@PathVariable Long id) {
        Long coupleId = currentUser.getCoupleId();
        travelService.deleteTravel(id, coupleId);
        return Result.success("删除成功", null);
    }

    @PostMapping("/{id}/photos")
    public Result<TravelPhoto> addPhoto(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = currentUser.getUserId();
        Long coupleId = currentUser.getCoupleId();
        return Result.success("照片已添加", travelService.addPhoto(userId, coupleId, id, body.get("photoUrl")));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public Result<Void> deletePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        Long coupleId = currentUser.getCoupleId();
        travelService.deletePhoto(photoId, coupleId);
        return Result.success("删除成功", null);
    }
}
