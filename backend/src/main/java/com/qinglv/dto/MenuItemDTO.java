package com.qinglv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MenuItemDTO {

    @NotNull(message = "日期不能为空")
    private LocalDate mealDate;

    @NotBlank(message = "餐次不能为空")
    private String mealType;

    @NotBlank(message = "菜名不能为空")
    @Size(max = 100, message = "菜名不能超过100字")
    private String dishName;
}
