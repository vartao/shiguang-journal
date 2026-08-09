package com.qinglv.dto;

import lombok.Data;

/**
 * 地图点亮聚合：每个省份去过/想去的足迹数量
 */
@Data
public class TravelMapSummaryVO {

    private String province;

    private Integer visited;

    private Integer planned;
}
