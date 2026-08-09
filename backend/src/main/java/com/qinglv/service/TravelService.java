package com.qinglv.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qinglv.common.BusinessException;
import com.qinglv.dto.TravelDTO;
import com.qinglv.dto.TravelMapSummaryVO;
import com.qinglv.entity.Travel;
import com.qinglv.entity.TravelPhoto;
import com.qinglv.mapper.TravelMapper;
import com.qinglv.mapper.TravelPhotoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 旅行足迹服务 - 足迹与照片管理、地图点亮聚合
 */
@Service
@RequiredArgsConstructor
public class TravelService {

    private final TravelMapper travelMapper;
    private final TravelPhotoMapper travelPhotoMapper;

    /**
     * 获取足迹列表（按出行日期倒序），附带照片数量
     */
    public List<Travel> getTravels(Long coupleId, Integer status) {
        requireCouple(coupleId);

        LambdaQueryWrapper<Travel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Travel::getCoupleId, coupleId);
        if (status != null) {
            wrapper.eq(Travel::getStatus, status);
        }
        wrapper.orderByDesc(Travel::getTravelDate)
               .orderByDesc(Travel::getCreateTime);

        List<Travel> travels = travelMapper.selectList(wrapper);
        fillPhotoCounts(travels);
        return travels;
    }

    /**
     * 地图点亮聚合：按省份统计去过/想去数量
     */
    public List<TravelMapSummaryVO> getMapSummary(Long coupleId) {
        requireCouple(coupleId);

        List<Travel> travels = travelMapper.selectList(
                new LambdaQueryWrapper<Travel>().eq(Travel::getCoupleId, coupleId)
        );

        Map<String, TravelMapSummaryVO> summaryMap = new LinkedHashMap<>();
        for (Travel travel : travels) {
            TravelMapSummaryVO item = summaryMap.computeIfAbsent(travel.getProvince(), province -> {
                TravelMapSummaryVO vo = new TravelMapSummaryVO();
                vo.setProvince(province);
                vo.setVisited(0);
                vo.setPlanned(0);
                return vo;
            });
            if (Objects.equals(travel.getStatus(), 0)) {
                item.setPlanned(item.getPlanned() + 1);
            } else {
                item.setVisited(item.getVisited() + 1);
            }
        }

        List<TravelMapSummaryVO> summaries = new ArrayList<>(summaryMap.values());
        summaries.sort((a, b) -> {
            int totalDiff = (b.getVisited() + b.getPlanned()) - (a.getVisited() + a.getPlanned());
            return totalDiff != 0 ? totalDiff : a.getProvince().compareTo(b.getProvince());
        });
        return summaries;
    }

    /**
     * 获取足迹详情（含照片列表）
     */
    public Travel getTravelDetail(Long travelId, Long coupleId) {
        Travel travel = getTravel(travelId, coupleId);
        List<TravelPhoto> photos = travelPhotoMapper.selectList(
                new LambdaQueryWrapper<TravelPhoto>()
                        .eq(TravelPhoto::getTravelId, travelId)
                        .orderByAsc(TravelPhoto::getSortOrder)
                        .orderByAsc(TravelPhoto::getCreateTime)
        );
        travel.setPhotos(photos);
        travel.setPhotoCount(photos.size());
        return travel;
    }

    /**
     * 创建足迹；若上传了封面图，同时把它作为第一张照片
     */
    @Transactional(rollbackFor = Exception.class)
    public Travel createTravel(Long userId, Long coupleId, TravelDTO dto) {
        requireCouple(coupleId);

        Travel travel = new Travel();
        travel.setCoupleId(coupleId);
        travel.setUserId(userId);
        applyDto(travel, dto);
        travel.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        travel.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        travelMapper.insert(travel);

        // 封面图同步作为第一张旅行照片，避免详情打开是空的
        if (travel.getCover() != null && !travel.getCover().isBlank()) {
            TravelPhoto photo = new TravelPhoto();
            photo.setTravelId(travel.getId());
            photo.setCoupleId(coupleId);
            photo.setPhotoUrl(travel.getCover());
            photo.setSortOrder(0);
            travelPhotoMapper.insert(photo);
        }
        travel.setPhotoCount(travel.getCover() != null && !travel.getCover().isBlank() ? 1 : 0);
        return travel;
    }

    /**
     * 更新足迹（双方均可编辑）
     */
    public Travel updateTravel(Long travelId, Long coupleId, TravelDTO dto) {
        Travel travel = getTravel(travelId, coupleId);
        applyDto(travel, dto);
        if (dto.getStatus() != null) {
            travel.setStatus(dto.getStatus());
        }
        if (dto.getSortOrder() != null) {
            travel.setSortOrder(dto.getSortOrder());
        }
        travelMapper.updateById(travel);
        return travel;
    }

    /**
     * 删除足迹（连带删除照片）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTravel(Long travelId, Long coupleId) {
        getTravel(travelId, coupleId);
        travelPhotoMapper.delete(
                new LambdaQueryWrapper<TravelPhoto>().eq(TravelPhoto::getTravelId, travelId)
        );
        travelMapper.deleteById(travelId);
    }

    /**
     * 通过已上传的图片 URL 添加旅行照片；封面为空时回填封面
     */
    @Transactional(rollbackFor = Exception.class)
    public TravelPhoto addPhoto(Long userId, Long coupleId, Long travelId, String photoUrl) {
        requireCouple(coupleId);
        if (photoUrl == null || photoUrl.isBlank()) {
            throw new BusinessException("照片地址不能为空");
        }

        Travel travel = getTravel(travelId, coupleId);

        TravelPhoto photo = new TravelPhoto();
        photo.setTravelId(travelId);
        photo.setCoupleId(coupleId);
        photo.setPhotoUrl(photoUrl);
        photo.setSortOrder(0);
        travelPhotoMapper.insert(photo);

        if (travel.getCover() == null || travel.getCover().isBlank()) {
            travel.setCover(photoUrl);
            travelMapper.updateById(travel);
        }
        return photo;
    }

    /**
     * 删除旅行照片；若删除的是封面则顺延或清空封面
     */
    @Transactional(rollbackFor = Exception.class)
    public void deletePhoto(Long photoId, Long coupleId) {
        TravelPhoto photo = travelPhotoMapper.selectById(photoId);
        if (photo == null) {
            throw new BusinessException("照片不存在");
        }
        if (!Objects.equals(photo.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权删除该照片");
        }

        travelPhotoMapper.deleteById(photoId);

        Travel travel = travelMapper.selectById(photo.getTravelId());
        if (travel != null && Objects.equals(travel.getCover(), photo.getPhotoUrl())) {
            TravelPhoto replacement = travelPhotoMapper.selectOne(
                    new LambdaQueryWrapper<TravelPhoto>()
                            .eq(TravelPhoto::getTravelId, travel.getId())
                            .orderByAsc(TravelPhoto::getCreateTime)
                            .last("LIMIT 1")
            );
            travel.setCover(replacement == null ? null : replacement.getPhotoUrl());
            travelMapper.updateById(travel);
        }
    }

    private Travel getTravel(Long travelId, Long coupleId) {
        Travel travel = travelMapper.selectById(travelId);
        if (travel == null) {
            throw new BusinessException("足迹不存在");
        }
        if (!Objects.equals(travel.getCoupleId(), coupleId)) {
            throw new BusinessException(403, "无权访问该足迹");
        }
        return travel;
    }

    private void applyDto(Travel travel, TravelDTO dto) {
        travel.setTitle(dto.getTitle());
        travel.setProvince(dto.getProvince());
        travel.setCity(dto.getCity());
        travel.setLatitude(dto.getLatitude());
        travel.setLongitude(dto.getLongitude());
        travel.setTravelDate(dto.getTravelDate());
        travel.setWeather(dto.getWeather());
        travel.setNote(dto.getNote());
        travel.setCover(dto.getCover());
        // 季节未填写时根据出行月份推导，保证按季节分组稳定
        if (dto.getSeason() != null && !dto.getSeason().isBlank()) {
            travel.setSeason(dto.getSeason());
        } else {
            travel.setSeason(deriveSeason(dto.getTravelDate()));
        }
    }

    private String deriveSeason(LocalDate date) {
        if (date == null) {
            return null;
        }
        int month = date.getMonthValue();
        if (month >= 3 && month <= 5) return "春";
        if (month >= 6 && month <= 8) return "夏";
        if (month >= 9 && month <= 11) return "秋";
        return "冬";
    }

    private void fillPhotoCounts(List<Travel> travels) {
        if (travels.isEmpty()) {
            return;
        }
        List<Long> ids = travels.stream().map(Travel::getId).collect(Collectors.toList());
        List<TravelPhoto> photos = travelPhotoMapper.selectList(
                new LambdaQueryWrapper<TravelPhoto>()
                        .in(TravelPhoto::getTravelId, ids)
                        .select(TravelPhoto::getTravelId)
        );
        Map<Long, Long> counts = photos.stream()
                .collect(Collectors.groupingBy(TravelPhoto::getTravelId, Collectors.counting()));
        for (Travel travel : travels) {
            travel.setPhotoCount(counts.getOrDefault(travel.getId(), 0L).intValue());
        }
    }

    private void requireCouple(Long coupleId) {
        if (coupleId == null) {
            throw new BusinessException("尚未加入情侣空间");
        }
    }
}
