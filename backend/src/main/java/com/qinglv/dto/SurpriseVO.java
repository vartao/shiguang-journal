package com.qinglv.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 惊喜彩蛋VO
 */
@Data
public class SurpriseVO {

    private Long id;
    private Long coupleId;
    private Long userId;
    private String creatorNickname;
    /** 前端兼容 fromName */
    private String fromName;

    private String title;
    private String content;
    private String hint;
    private String coverImage;
    private String unlockCondition;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate unlockDate;

    /**
     * 是否已解锁：0-未解锁 1-已解锁
     */
    private Integer unlocked;

    /**
     * 前端布尔兼容（JSON 序列化时额外输出 unlocked 的布尔语义由前端 || 处理）
     */
    @JsonProperty("isUnlocked")
    public boolean isUnlockedFlag() {
        return unlocked != null && unlocked == 1;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime unlockTime;

    private Long unlockedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 前端兼容 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
