package com.qinglv.service;

import com.qinglv.common.BusinessException;
import com.qinglv.dto.TravelMapSummaryVO;
import com.qinglv.entity.Travel;
import com.qinglv.entity.TravelPhoto;
import com.qinglv.mapper.TravelMapper;
import com.qinglv.mapper.TravelPhotoMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelServiceTest {

    @Mock
    private TravelMapper travelMapper;
    @Mock
    private TravelPhotoMapper travelPhotoMapper;

    @InjectMocks
    private TravelService travelService;

    private Travel travel;

    @BeforeEach
    void setUp() {
        travel = new Travel();
        travel.setId(10L);
        travel.setCoupleId(100L);
        travel.setUserId(1L);
        travel.setProvince("云南");
        travel.setCity("大理");
        travel.setTitle("苍山洱海");
        travel.setStatus(1);
    }

    private Travel travelOf(String province, Integer status) {
        Travel item = new Travel();
        item.setProvince(province);
        item.setStatus(status);
        return item;
    }

    @Test
    void mapSummaryAggregatesVisitedAndPlannedByProvince() {
        when(travelMapper.selectList(any())).thenReturn(List.of(
                travelOf("云南", 1),
                travelOf("云南", 1),
                travelOf("云南", 0),
                travelOf("四川", 1),
                travelOf("海南", 0)
        ));

        List<TravelMapSummaryVO> summary = travelService.getMapSummary(100L);

        assertEquals(3, summary.size());
        TravelMapSummaryVO yunnan = summary.stream()
                .filter(item -> "云南".equals(item.getProvince())).findFirst().orElseThrow();
        assertEquals(2, yunnan.getVisited());
        assertEquals(1, yunnan.getPlanned());
        // 总量最多的省份排在最前
        assertEquals("云南", summary.get(0).getProvince());
        TravelMapSummaryVO hainan = summary.stream()
                .filter(item -> "海南".equals(item.getProvince())).findFirst().orElseThrow();
        assertEquals(0, hainan.getVisited());
        assertEquals(1, hainan.getPlanned());
    }

    @Test
    void mapSummaryRequiresCouple() {
        BusinessException error = assertThrows(
                BusinessException.class, () -> travelService.getMapSummary(null));

        assertEquals("尚未加入情侣空间", error.getMessage());
    }

    @Test
    void travelOfAnotherCoupleIsRejected() {
        travel.setCoupleId(999L);
        when(travelMapper.selectById(10L)).thenReturn(travel);

        BusinessException error = assertThrows(
                BusinessException.class, () -> travelService.getTravelDetail(10L, 100L));
        assertEquals(403, error.getCode());

        error = assertThrows(
                BusinessException.class, () -> travelService.deleteTravel(10L, 100L));
        assertEquals(403, error.getCode());
        verify(travelMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void missingTravelThrowsNotFound() {
        when(travelMapper.selectById(10L)).thenReturn(null);

        BusinessException error = assertThrows(
                BusinessException.class, () -> travelService.getTravelDetail(10L, 100L));
        assertEquals("足迹不存在", error.getMessage());
    }

    @Test
    void addPhotoBackfillsEmptyCover() {
        travel.setCover(null);
        when(travelMapper.selectById(10L)).thenReturn(travel);

        TravelPhoto photo = travelService.addPhoto(1L, 100L, 10L, "/uploads/first.jpg");

        assertEquals("/uploads/first.jpg", photo.getPhotoUrl());
        assertEquals("/uploads/first.jpg", travel.getCover());
        verify(travelPhotoMapper).insert(photo);
        verify(travelMapper).updateById(travel);
    }

    @Test
    void addPhotoKeepsExistingCover() {
        travel.setCover("/uploads/cover.jpg");
        when(travelMapper.selectById(10L)).thenReturn(travel);

        travelService.addPhoto(1L, 100L, 10L, "/uploads/second.jpg");

        assertEquals("/uploads/cover.jpg", travel.getCover());
        verify(travelMapper, never()).updateById(any(Travel.class));
    }

    @Test
    void deletingCoverPhotoSelectsReplacement() {
        TravelPhoto deleted = new TravelPhoto();
        deleted.setId(7L);
        deleted.setTravelId(10L);
        deleted.setCoupleId(100L);
        deleted.setPhotoUrl("/uploads/old-cover.jpg");
        travel.setCover(deleted.getPhotoUrl());

        TravelPhoto replacement = new TravelPhoto();
        replacement.setPhotoUrl("/uploads/replacement.jpg");
        when(travelPhotoMapper.selectById(7L)).thenReturn(deleted);
        when(travelMapper.selectById(10L)).thenReturn(travel);
        when(travelPhotoMapper.selectOne(any())).thenReturn(replacement);

        travelService.deletePhoto(7L, 100L);

        assertEquals("/uploads/replacement.jpg", travel.getCover());
        verify(travelPhotoMapper).deleteById(7L);
        verify(travelMapper).updateById(travel);
    }

    @Test
    void deletingPhotoFromAnotherCoupleIsRejected() {
        TravelPhoto photo = new TravelPhoto();
        photo.setId(7L);
        photo.setCoupleId(999L);
        when(travelPhotoMapper.selectById(7L)).thenReturn(photo);

        BusinessException error = assertThrows(
                BusinessException.class, () -> travelService.deletePhoto(7L, 100L));

        assertEquals(403, error.getCode());
        verify(travelPhotoMapper, never()).deleteById(any(Long.class));
    }
}
