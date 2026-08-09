package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ql_dish")
public class Dish {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long userId;

    private String name;

    private String category;

    private String note;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
