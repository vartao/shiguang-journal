package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 情侣空间功能开关
 */
@Data
@TableName("ql_couple_feature")
public class CoupleFeature {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long coupleId;

    private String featureKey;

    private Integer enabled;
}
