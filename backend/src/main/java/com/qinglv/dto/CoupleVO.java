package com.qinglv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 情侣空间信息VO（含在一起天数）
 */
@Data
public class CoupleVO {

    private Long id;
    private String name;
    private String avatar;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate togetherDate;

    /** 前端兼容 startDate */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    private Long togetherDays;

    private Long userId1;
    private Long userId2;
    private String inviteCode;
    private String announcement;

    private String user1Nickname;
    private String user1Avatar;
    private String user2Nickname;
    private String user2Avatar;

    /** 相对当前用户的另一半昵称 */
    private String partnerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
