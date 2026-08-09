package com.qinglv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageVO {

    private Long id;
    private Long userId;
    private String content;
    private String nickname;
    private String avatar;
    private LocalDateTime createTime;
}
