package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 笔记图片实体
 */
@Data
@TableName("ql_note_image")
public class NoteImage implements Serializable {

    /**
     * 图片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属笔记ID
     */
    private Long noteId;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
