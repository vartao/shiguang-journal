package com.qinglv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CapsuleVO {

    private Long id;
    private Long coupleId;
    private Long userId;
    private String title;

    /** 未开启且未到期时为 null（防泄露） */
    private String message;

    /** 未开启且未到期时为 null（防泄露） */
    private List<String> photoUrls;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate openDate;

    private Integer isOpened;
    private Long openedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime openedTime;

    /** 未开启且已到开启日期 */
    private Boolean canOpen;

    /** 距开启天数，已开启或已到期为 0 */
    private Long daysLeft;

    private String creatorNickname;
    private String creatorAvatar;
    private String openerNickname;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
