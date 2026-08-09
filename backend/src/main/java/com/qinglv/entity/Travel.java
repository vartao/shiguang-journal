package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("ql_travel")
public class Travel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private Long userId;

    private String province;

    private String city;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private LocalDate travelDate;

    private String season;

    private String weather;

    private String title;

    private String note;

    private String cover;

    private Integer status;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 照片数量（列表/详情时填充，非数据库字段） */
    @TableField(exist = false)
    private Integer photoCount;

    /** 照片列表（仅详情时填充，非数据库字段） */
    @TableField(exist = false)
    private List<TravelPhoto> photos;
}
