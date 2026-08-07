package com.qinglv.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 笔记实体
 */
@Data
@TableName("ql_note")
public class Note implements Serializable {

    /**
     * 笔记ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属情侣空间ID
     */
    private Long coupleId;

    /**
     * 作者用户ID
     */
    private Long userId;

    /**
     * 笔记标题
     */
    private String title;

    /**
     * 笔记内容
     */
    private String content;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 心情标签：happy-开心 sad-难过 angry-生气 surprised-惊喜 tired-疲惫 loved-被爱
     */
    private String mood;

    /**
     * 天气：sunny-晴 cloudy-多云 rainy-雨 snowy-雪 windy-风
     */
    private String weather;

    /**
     * 是否置顶：0-否 1-是
     */
    private Integer isTop;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 用户选择的记录日期。
     */
    private LocalDate noteDate;

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
