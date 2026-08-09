package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ql_capsule")
public class Capsule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long userId;

    private String title;

    private String message;

    /** 照片URL JSON数组字符串 */
    private String photoUrls;

    private LocalDate openDate;

    /** 是否已开启：0-未开启 1-已开启 */
    private Integer isOpened;

    private Long openedBy;

    private LocalDateTime openedTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
