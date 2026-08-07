package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 纪念日实体
 */
@Data
@TableName("ql_anniversary")
public class Anniversary implements Serializable {

    /**
     * 纪念日ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属情侣空间ID
     */
    private Long coupleId;

    /**
     * 创建者用户ID
     */
    private Long userId;

    /**
     * 纪念日名称
     */
    private String name;

    /**
     * 纪念日日期
     */
    private LocalDate date;

    /**
     * 类型：love-恋爱 birthday-生日 custom-自定义
     */
    private String type;

    /**
     * 图标
     */
    private String icon;

    /**
     * 是否每年重复：0-否 1-是
     */
    private Integer repeatYearly;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
