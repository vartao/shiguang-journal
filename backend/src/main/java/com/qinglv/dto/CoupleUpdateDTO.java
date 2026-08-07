package com.qinglv.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新情侣空间请求DTO
 */
@Data
public class CoupleUpdateDTO {

    /**
     * 空间名称
     */
    @Size(max = 50, message = "空间名称最长50个字符")
    private String name;

    /**
     * 空间头像/封面
     */
    private String avatar;

    /**
     * 在一起日期（格式 yyyy-MM-dd）
     */
    private String togetherDate;

    /**
     * 空间公告/签名
     */
    @Size(max = 200, message = "公告最长200个字符")
    private String announcement;
}
