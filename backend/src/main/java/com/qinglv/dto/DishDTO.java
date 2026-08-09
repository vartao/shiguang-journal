package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DishDTO {

    @NotBlank(message = "菜名不能为空")
    @Size(max = 100, message = "菜名不能超过100字")
    private String name;

    private String category;

    @Size(max = 500, message = "备注不能超过500字")
    private String note;
}
