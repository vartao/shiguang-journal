package com.qinglv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记详情VO（含图片列表、作者信息）
 * 部分字段为前端兼容别名
 */
@Data
public class NoteVO {

    private Long id;
    private Long coupleId;
    private Long userId;

    private String authorNickname;
    /** 前端 HomeView 使用 authorName */
    private String authorName;
    private String authorAvatar;

    private String title;
    private String content;
    private String coverImage;
    private String mood;
    private String weather;
    private Integer isTop;
    private Integer likeCount;
    /** 前端 HomeView 使用 likes */
    private Integer likes;
    /** 当前用户是否已点赞 */
    private Boolean liked;

    private List<String> images;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** 前端兼容 createdAt */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 前端兼容 noteDate */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate noteDate;
}
