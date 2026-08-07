package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WishDTO {

    @NotBlank(message = "心愿标题不能为空")
    @Size(max = 200, message = "标题不能超过200字")
    private String title;

    @Size(max = 1000, message = "描述不能超过1000字")
    private String description;

    private String icon;

    private String category;

    private Integer priority;
}
