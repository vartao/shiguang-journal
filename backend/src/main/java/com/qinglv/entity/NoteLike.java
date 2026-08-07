package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 笔记点赞记录（用户维度去重）
 */
@Data
@TableName("ql_note_like")
public class NoteLike implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
