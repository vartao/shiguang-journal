package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 相册实体
 */
@Data
@TableName("ql_album")
public class Album implements Serializable {

    /**
     * 相册ID
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
     * 相册名称
     */
    private String name;

    /**
     * 相册描述
     */
    private String description;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 照片数量
     */
    private Integer photoCount;

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
