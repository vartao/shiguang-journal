package com.qinglv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WishVO {

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
    private String creatorNickname;
    private String creatorAvatar;
    private String completerNickname;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
