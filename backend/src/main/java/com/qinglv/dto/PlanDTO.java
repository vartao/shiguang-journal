package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PlanDTO {

    @NotBlank(message = "行程标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 200, message = "目的地不能超过200字")
    private String destination;

    @Size(max = 500, message = "封面地址不能超过500字")
    private String cover;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal budget;

    /** 每日安排JSON字符串，service 层校验合法性 */
    private String days;

    @Size(max = 1000, message = "备注不能超过1000字")
    private String note;

    /** 状态：0-计划中 1-已出发 2-已完成 */
    private Integer status;
}
