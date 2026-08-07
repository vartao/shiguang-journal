package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ql_wish")
public class Wish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long userId;

    private String title;

    private String description;

    private String icon;

    private String category;

    private Integer priority;

    private Integer status;

    private LocalDateTime completedTime;

    private Long completedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
