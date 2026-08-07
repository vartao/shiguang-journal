package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣空间实体
 */
@Data
@TableName("ql_couple")
public class Couple implements Serializable {

    /**
     * 空间ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 空间名称
     */
    private String name;

    /**
     * 空间头像/封面
     */
    private String avatar;

    /**
     * 在一起日期
     */
    private LocalDate togetherDate;

    /**
     * 男方用户ID
     */
    private Long userId1;

    /**
     * 女方用户ID
     */
    private Long userId2;

    /**
     * 空间邀请码
     */
    private String inviteCode;

    /**
     * 空间公告/签名
     */
    private String announcement;

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
