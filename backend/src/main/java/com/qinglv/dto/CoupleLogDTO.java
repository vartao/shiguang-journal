package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 创建/编辑小事记请求DTO
 */
@Data
public class CoupleLogDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    /** 分类：movie/food/show/travel/date/sport/other，非法值由服务端落为 other */
    private String category;

    @NotNull(message = "发生日期不能为空")
    private LocalDate logDate;

    @Size(max = 2000, message = "手记不能超过2000字")
    private String note;

    @Size(max = 500, message = "封面图URL过长")
    private String cover;

    /** 评分：0-5，超出范围由服务端拒绝 */
    private Integer rating;
}
