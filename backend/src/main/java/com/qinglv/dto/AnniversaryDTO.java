package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建/编辑纪念日请求DTO
 */
@Data
public class AnniversaryDTO {

    /**
     * 纪念日名称
     */
    @Size(max = 50, message = "纪念日名称最长50个字符")
    private String name;

    /**
     * 前端兼容 title
     */
    @Size(max = 50, message = "纪念日名称最长50个字符")
    private String title;

    /**
     * 纪念日日期（格式 yyyy-MM-dd）
     */
    @NotBlank(message = "纪念日日期不能为空")
    private String date;

    /**
     * 类型：love-恋爱 birthday-生日 custom-自定义
     */
    private String type;

    /**
     * 图标
     */
    private String icon;

    /**
     * 是否每年重复：0-否 1-是
     */
    private Integer repeatYearly;

    /**
     * 备注
     */
    @Size(max = 200, message = "备注最长200个字符")
    private String remark;
}
