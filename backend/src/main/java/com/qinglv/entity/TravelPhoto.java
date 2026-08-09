package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ql_travel_photo")
public class TravelPhoto {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long travelId;

    private Long coupleId;

    private String photoUrl;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
