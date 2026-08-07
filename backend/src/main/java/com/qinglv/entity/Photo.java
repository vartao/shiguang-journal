package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 照片实体
 */
@Data
@TableName("ql_photo")
public class Photo implements Serializable {

    /**
     * 照片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属相册ID
     */
    private Long albumId;

    /**
     * 所属情侣空间ID
     */
    private Long coupleId;

    /**
     * 上传者用户ID
     */
    private Long userId;

    /**
     * 照片URL
     */
    private String photoUrl;

    /**
     * 缩略图URL
     */
    private String thumbnailUrl;

    /**
     * 照片描述
     */
    private String description;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
