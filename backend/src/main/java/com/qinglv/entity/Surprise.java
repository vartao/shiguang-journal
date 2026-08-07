package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 惊喜彩蛋实体
 */
@Data
@TableName("ql_surprise")
public class Surprise implements Serializable {

    /**
     * 彩蛋ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属情侣空间ID
     */
    private Long coupleId;

    /**
     * 创建者用户ID（送出方）
     */
    private Long userId;

    /**
     * 彩蛋标题
     */
    private String title;

    /**
     * 彩蛋内容（解锁后可见）
     */
    private String content;

    /**
     * 封面/提示语（未解锁时展示）
     */
    private String hint;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 解锁条件类型：date-到期自动解锁 password-密码解锁 click-点击解锁
     */
    private String unlockCondition;

    /**
     * 解锁日期（unlock_condition=date时使用）
     */
    private LocalDate unlockDate;

    /**
     * 解锁密码（unlock_condition=password时使用，加密存储）
     */
    private String unlockPassword;

    /**
     * 是否已解锁：0-未解锁 1-已解锁
     */
    private Integer unlocked;

    /**
     * 解锁时间
     */
    private LocalDateTime unlockTime;

    /**
     * 解锁者用户ID
     */
    private Long unlockedBy;

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
