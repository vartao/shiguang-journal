package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/编辑惊喜彩蛋请求DTO
 */
@Data
public class SurpriseDTO {

    /**
     * 彩蛋标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    /**
     * 彩蛋内容（解锁后可见）
     */
    @NotBlank(message = "内容不能为空")
    private String content;

    /**
     * 封面/提示语（未解锁时展示）
     */
    private String hint;

    /**
     * 封面图URL
     */
    private String coverImage;

    /**
     * 解锁条件类型：date-到期自动解锁 password-密码解锁 click-点击解锁
     */
    @NotBlank(message = "解锁条件不能为空")
    private String unlockCondition;

    /**
     * 解锁日期（格式 yyyy-MM-dd，unlock_condition=date时使用）
     */
    private String unlockDate;

    /**
     * 解锁密码（unlock_condition=password时使用）
     */
    private String unlockPassword;
}
