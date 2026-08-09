package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ql_plan")
public class Plan {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long userId;

    private String title;

    private String destination;

    private String cover;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal budget;

    /** 每日安排JSON字符串：[{date,items:[text]}] */
    private String days;

    private String note;

    /** 状态：0-计划中 1-已出发 2-已完成 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
