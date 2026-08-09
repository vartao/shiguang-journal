package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣日志（小事记）：一起做过的事打卡
 */
@Data
@TableName("ql_couple_log")
public class CoupleLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long userId;

    /** 分类：movie-电影 food-美食 show-演出 travel-旅行 date-约会 sport-运动 other-其他 */
    private String category;

    private String title;

    /** 发生日期 */
    private LocalDate logDate;

    /** 一句话手记 */
    private String note;

    /** 封面图URL */
    private String cover;

    /** 评分：0-5 */
    private Integer rating;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
